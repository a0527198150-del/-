package com.example.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.ApprovedChannel
import com.example.data.network.ApiKeyCheck
import com.example.data.network.YoutubeDataSource
import com.example.data.network.YoutubeVideo
import com.example.data.repository.AddChannelResult
import com.example.data.repository.ChannelRepository
import com.example.data.settings.AppSettings
import com.example.data.settings.SettingsRepository
import com.example.data.settings.WatchProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Loading state shared by every list of videos in the app. */
sealed interface VideosUiState {
    object Idle : VideosUiState
    object Loading : VideosUiState
    data class Success(val videos: List<YoutubeVideo>) : VideosUiState
    data class Error(val message: String) : VideosUiState
}

/** Where the user currently is. A small hand-rolled stack keeps back behaviour obvious. */
sealed interface Destination {
    object Home : Destination
    data class ChannelDetail(val channel: ApprovedChannel) : Destination
    data class Player(val video: YoutubeVideo, val startSeconds: Float = 0f) : Destination
    object Supervisor : Destination
}

enum class HomeTab { FEED, CHANNELS, SEARCH }

/** Gate shown before the app proper: splash while reading settings, then setup. */
sealed interface LaunchState {
    object Loading : LaunchState
    object NeedsSetup : LaunchState
    object Ready : LaunchState
}

/** Live feedback while the supervisor types an API key. */
sealed interface KeyCheckState {
    object Idle : KeyCheckState
    object Checking : KeyCheckState
    object Valid : KeyCheckState
    data class Invalid(val message: String) : KeyCheckState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val youtube = YoutubeDataSource()
    private val repository = ChannelRepository(
        channelDao = AppDatabase.getDatabase(application).channelDao(),
        settings = settingsRepository,
        youtube = youtube,
    )

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val approvedChannels: StateFlow<List<ApprovedChannel>> = repository.approvedChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val continueWatching: StateFlow<List<WatchProgress>> = settingsRepository.watchProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _launchState = MutableStateFlow<LaunchState>(LaunchState.Loading)
    val launchState: StateFlow<LaunchState> = _launchState.asStateFlow()

    private val backStack = ArrayDeque<Destination>()

    private val _destination = MutableStateFlow<Destination>(Destination.Home)
    val destination: StateFlow<Destination> = _destination.asStateFlow()

    private val _homeTab = MutableStateFlow(HomeTab.FEED)
    val homeTab: StateFlow<HomeTab> = _homeTab.asStateFlow()

    private val _feedState = MutableStateFlow<VideosUiState>(VideosUiState.Idle)
    val feedState: StateFlow<VideosUiState> = _feedState.asStateFlow()

    private val _channelVideos = MutableStateFlow<VideosUiState>(VideosUiState.Idle)
    val channelVideos: StateFlow<VideosUiState> = _channelVideos.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSupervisor = MutableStateFlow(false)
    val isSupervisor: StateFlow<Boolean> = _isSupervisor.asStateFlow()

    private val _showPinPrompt = MutableStateFlow(false)
    val showPinPrompt: StateFlow<Boolean> = _showPinPrompt.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    private val _keyCheck = MutableStateFlow<KeyCheckState>(KeyCheckState.Idle)
    val keyCheck: StateFlow<KeyCheckState> = _keyCheck.asStateFlow()

    private val _isAddingChannel = MutableStateFlow(false)
    val isAddingChannel: StateFlow<Boolean> = _isAddingChannel.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Supervisor form fields.
    val inputChannelId = MutableStateFlow("")
    val inputCustomName = MutableStateFlow("")
    val inputDescription = MutableStateFlow("")
    val inputNewPin = MutableStateFlow("")
    val inputPinConfirm = MutableStateFlow("")

    init {
        viewModelScope.launch {
            repository.seedDefaultChannelsIfEmpty()
            val stored = settingsRepository.settings.first()
            _launchState.value = if (stored.setupCompleted) LaunchState.Ready else LaunchState.NeedsSetup
            if (stored.setupCompleted) refreshFeed()
        }
    }

    // region setup

    fun saveApiKeyAndContinue(rawKey: String, verify: Boolean) {
        val key = rawKey.trim()
        viewModelScope.launch {
            if (verify && key.isNotBlank()) {
                _keyCheck.value = KeyCheckState.Checking
                when (val result = youtube.checkApiKey(key)) {
                    is ApiKeyCheck.Invalid -> {
                        _keyCheck.value = KeyCheckState.Invalid(result.reason)
                        return@launch
                    }

                    ApiKeyCheck.Valid -> _keyCheck.value = KeyCheckState.Valid
                }
            }
            settingsRepository.setApiKey(key)
            settingsRepository.completeSetup()
            _launchState.value = LaunchState.Ready
            if (key.isNotBlank()) {
                launch { repository.refreshChannelMetadata(onlyMissing = true) }
            }
            refreshFeed()
        }
    }

    fun skipApiKey() {
        viewModelScope.launch {
            settingsRepository.completeSetup()
            _launchState.value = LaunchState.Ready
            refreshFeed()
        }
    }

    fun updateApiKey(rawKey: String) {
        val key = rawKey.trim()
        viewModelScope.launch {
            _keyCheck.value = KeyCheckState.Checking
            if (key.isBlank()) {
                settingsRepository.setApiKey("")
                _keyCheck.value = KeyCheckState.Idle
                _statusMessage.value = "המפתח הוסר. האפליקציה תשתמש בערוץ ה-RSS הציבורי."
                return@launch
            }
            when (val result = youtube.checkApiKey(key)) {
                is ApiKeyCheck.Invalid -> _keyCheck.value = KeyCheckState.Invalid(result.reason)
                ApiKeyCheck.Valid -> {
                    settingsRepository.setApiKey(key)
                    _keyCheck.value = KeyCheckState.Valid
                    _statusMessage.value = "המפתח נשמר ואומת בהצלחה."
                    repository.refreshChannelMetadata(onlyMissing = true)
                    refreshFeed()
                }
            }
        }
    }

    fun resetKeyCheck() {
        _keyCheck.value = KeyCheckState.Idle
    }

    // endregion

    // region navigation

    fun selectTab(tab: HomeTab) {
        _homeTab.value = tab
        if (tab == HomeTab.FEED && _feedState.value is VideosUiState.Idle) refreshFeed()
    }

    fun openChannel(channel: ApprovedChannel) {
        navigate(Destination.ChannelDetail(channel))
        loadChannelVideos(channel.channelId)
    }

    fun playVideo(video: YoutubeVideo, startSeconds: Float = 0f) {
        navigate(Destination.Player(video, startSeconds))
    }

    fun resumeWatching(progress: WatchProgress) {
        playVideo(
            YoutubeVideo(
                id = progress.videoId,
                title = progress.title,
                channelId = progress.channelId,
                channelName = progress.channelName,
                thumbnailUrl = progress.thumbnailUrl,
                description = "",
                publishedAt = "",
                durationSeconds = progress.durationSeconds.toInt(),
            ),
            startSeconds = progress.positionSeconds,
        )
    }

    fun openSupervisor() {
        if (_isSupervisor.value) navigate(Destination.Supervisor) else _showPinPrompt.value = true
    }

    private fun navigate(destination: Destination) {
        backStack.addLast(_destination.value)
        _destination.value = destination
    }

    /** Returns false when there is nothing left to pop, so the Activity can finish. */
    fun navigateBack(): Boolean {
        val previous = backStack.removeLastOrNull() ?: return false
        _destination.value = previous
        return true
    }

    fun goHome() {
        backStack.clear()
        _destination.value = Destination.Home
    }

    // endregion

    // region content

    fun refreshFeed() {
        viewModelScope.launch {
            _feedState.value = VideosUiState.Loading
            try {
                val videos = repository.fetchLatestAcrossChannels()
                _feedState.value = if (videos.isEmpty()) {
                    VideosUiState.Error("לא הצלחנו לטעון סרטונים. בדוק את החיבור לאינטרנט או הוסף מפתח API.")
                } else {
                    VideosUiState.Success(videos)
                }
            } catch (error: Exception) {
                _feedState.value = VideosUiState.Error("שגיאה בטעינת הסרטונים: ${error.localizedMessage}")
            }
        }
    }

    fun loadChannelVideos(channelId: String) {
        viewModelScope.launch {
            _channelVideos.value = VideosUiState.Loading
            try {
                val videos = repository.fetchChannelVideos(channelId)
                _channelVideos.value = if (videos.isEmpty()) {
                    VideosUiState.Error("לא נמצאו סרטונים בערוץ זה, או שאין חיבור לרשת כרגע.")
                } else {
                    VideosUiState.Success(videos)
                }
            } catch (error: Exception) {
                _channelVideos.value = VideosUiState.Error("שגיאה בטעינת הערוץ: ${error.localizedMessage}")
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveProgress(video: YoutubeVideo, positionSeconds: Float, durationSeconds: Float) {
        viewModelScope.launch {
            settingsRepository.saveProgress(
                WatchProgress(
                    videoId = video.id,
                    title = video.title,
                    channelId = video.channelId,
                    channelName = video.channelName,
                    thumbnailUrl = video.highResThumbnail,
                    positionSeconds = positionSeconds,
                    durationSeconds = durationSeconds,
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    fun forgetProgress(videoId: String) {
        viewModelScope.launch { settingsRepository.forgetProgress(videoId) }
    }

    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch { settingsRepository.setPlaybackSpeed(speed) }
    }

    fun setAutoplay(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoplay(enabled) }
    }

    // endregion

    // region supervisor

    fun closePinPrompt() {
        _showPinPrompt.value = false
        _pinError.value = null
    }

    fun authenticateSupervisor(pin: String) {
        viewModelScope.launch {
            if (pin == repository.getSupervisorPin()) {
                _isSupervisor.value = true
                _showPinPrompt.value = false
                _pinError.value = null
                navigate(Destination.Supervisor)
            } else {
                _pinError.value = "קוד גישה שגוי. נסה שנית."
            }
        }
    }

    fun logoutSupervisor() {
        _isSupervisor.value = false
        goHome()
        _statusMessage.value = "יצאת ממצב מפקח."
    }

    fun addNewApprovedChannel() {
        val rawInput = inputChannelId.value.trim()
        if (rawInput.isEmpty()) {
            _statusMessage.value = "יש להזין קישור, שם משתמש או מזהה ערוץ."
            return
        }
        viewModelScope.launch {
            _isAddingChannel.value = true
            val result = try {
                repository.addChannel(rawInput, inputCustomName.value.trim(), inputDescription.value.trim())
            } catch (error: Exception) {
                AddChannelResult.Failed("שגיאה בחיבור ליוטיוב: ${error.localizedMessage}")
            }
            _isAddingChannel.value = false

            when (result) {
                is AddChannelResult.Added -> {
                    _statusMessage.value = "הערוץ '${result.channel.customName}' נוסף לרשימה המאושרת."
                    inputChannelId.value = ""
                    inputCustomName.value = ""
                    inputDescription.value = ""
                    refreshFeed()
                }

                is AddChannelResult.Failed -> _statusMessage.value = result.message
            }
        }
    }

    fun deleteChannel(channel: ApprovedChannel) {
        viewModelScope.launch {
            repository.removeChannel(channel)
            _statusMessage.value = "'${channel.customName}' הוסר מהרשימה המאושרת."
            refreshFeed()
        }
    }

    fun refreshChannelMetadata() {
        viewModelScope.launch {
            val updated = repository.refreshChannelMetadata(onlyMissing = false)
            _statusMessage.value = if (updated > 0) "עודכנו פרטים של $updated ערוצים." else "לא נמצאו פרטים חדשים לעדכון."
        }
    }

    fun changeSupervisorPin() {
        val newPin = inputNewPin.value.trim()
        when {
            newPin.length < 4 -> _statusMessage.value = "קוד המפקח חייב להכיל לפחות 4 ספרות."
            newPin != inputPinConfirm.value.trim() -> _statusMessage.value = "הקודים אינם תואמים."
            else -> viewModelScope.launch {
                repository.updateSupervisorPin(newPin)
                inputNewPin.value = ""
                inputPinConfirm.value = ""
                _statusMessage.value = "קוד המפקח עודכן בהצלחה."
            }
        }
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            settingsRepository.clearProgress()
            _statusMessage.value = "היסטוריית הצפייה נוקתה."
        }
    }

    // endregion

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
