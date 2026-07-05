package com.example.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.ApprovedChannel
import com.example.data.network.YoutubeVideo
import com.example.data.repository.ChannelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface VideosUiState {
    object Idle : VideosUiState
    object Loading : VideosUiState
    data class Success(val videos: List<YoutubeVideo>) : VideosUiState
    data class Error(val message: String) : VideosUiState
}

enum class ScreenState {
    CHANNELS,
    CHANNEL_VIDEOS,
    PLAYER,
    SUPERVISOR
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = ChannelRepository(database.channelDao())

    val approvedChannels: StateFlow<List<ApprovedChannel>> = repository.approvedChannels
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _screenState = MutableStateFlow(ScreenState.CHANNELS)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private val _selectedChannel = MutableStateFlow<ApprovedChannel?>(null)
    val selectedChannel: StateFlow<ApprovedChannel?> = _selectedChannel.asStateFlow()

    private val _selectedVideo = MutableStateFlow<YoutubeVideo?>(null)
    val selectedVideo: StateFlow<YoutubeVideo?> = _selectedVideo.asStateFlow()

    private val _videosState = MutableStateFlow<VideosUiState>(VideosUiState.Idle)
    val videosState: StateFlow<VideosUiState> = _videosState.asStateFlow()

    // Supervisor security states
    private val _isSupervisorAuthenticated = MutableStateFlow(false)
    val isSupervisorAuthenticated: StateFlow<Boolean> = _isSupervisorAuthenticated.asStateFlow()

    private val _showPinPrompt = MutableStateFlow(false)
    val showPinPrompt: StateFlow<Boolean> = _showPinPrompt.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    // Input state controls for supervisor actions
    val inputChannelId = MutableStateFlow("")
    val inputCustomName = MutableStateFlow("")
    val inputDescription = MutableStateFlow("")
    val inputNewPin = MutableStateFlow("")
    val inputPinConfirm = MutableStateFlow("")

    // User notification banner
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedDefaultChannelsIfEmpty()
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun navigateToChannels() {
        _screenState.value = ScreenState.CHANNELS
        _selectedChannel.value = null
        _selectedVideo.value = null
    }

    fun selectChannel(channel: ApprovedChannel) {
        _selectedChannel.value = channel
        _screenState.value = ScreenState.CHANNEL_VIDEOS
        loadVideos(channel.channelId)
    }

    fun playVideo(video: YoutubeVideo) {
        _selectedVideo.value = video
        _screenState.value = ScreenState.PLAYER
    }

    fun navigateToSupervisor() {
        if (_isSupervisorAuthenticated.value) {
            _screenState.value = ScreenState.SUPERVISOR
        } else {
            _showPinPrompt.value = true
        }
    }

    fun closePinPrompt() {
        _showPinPrompt.value = false
        _pinError.value = null
    }

    fun authenticateSupervisor(pin: String) {
        viewModelScope.launch {
            val correctPin = repository.getSupervisorPin()
            if (pin == correctPin) {
                _isSupervisorAuthenticated.value = true
                _showPinPrompt.value = false
                _pinError.value = null
                _screenState.value = ScreenState.SUPERVISOR
            } else {
                _pinError.value = "קוד גישה שגוי. נסה שנית."
            }
        }
    }

    fun logoutSupervisor() {
        _isSupervisorAuthenticated.value = false
        _screenState.value = ScreenState.CHANNELS
    }

    fun loadVideos(channelId: String) {
        viewModelScope.launch {
            _videosState.value = VideosUiState.Loading
            try {
                val videos = repository.fetchChannelVideos(channelId)
                if (videos.isEmpty()) {
                    _videosState.value = VideosUiState.Error("לא נמצאו סרטונים בערוץ או שאין חיבור לרשת. נסה שוב מאוחר יותר.")
                } else {
                    _videosState.value = VideosUiState.Success(videos)
                }
            } catch (e: Exception) {
                _videosState.value = VideosUiState.Error("שגיאה בטעינת סרטונים מהערוץ: ${e.localizedMessage}")
            }
        }
    }

    fun addNewApprovedChannel() {
        val rawId = inputChannelId.value.trim()
        val customName = inputCustomName.value.trim()
        val description = inputDescription.value.trim()

        if (rawId.isEmpty()) {
            _statusMessage.value = "שגיאה: יש להזין מזהה, קישור או שם משתמש של ערוץ"
            return
        }

        viewModelScope.launch {
            try {
                _statusMessage.value = "מפענח את מזהה הערוץ מיוטיוב... אנא המתן."
                val resolvedChannelId = repository.resolveChannelId(rawId)

                if (resolvedChannelId == null) {
                    _statusMessage.value = "שגיאה: לא הצלחנו לפענח מזהה ערוץ תקני מתוך הקלט. אנא ודא שהקישור/שם המשתמש נכונים או הזן מזהה ערוץ ישיר (המתחיל ב-UC)."
                    return@launch
                }

                _statusMessage.value = "הערוץ פוענח בהצלחה (ID: $resolvedChannelId). מוודא נתונים מיוטיוב..."
                val testVideos = repository.fetchChannelVideos(resolvedChannelId)
                val officialName = if (testVideos.isNotEmpty()) {
                    testVideos[0].channelName
                } else {
                    customName.ifBlank { "ערוץ מאושר חדש" }
                }

                repository.addChannel(
                    channelId = resolvedChannelId,
                    customName = customName.ifBlank { officialName },
                    officialName = officialName,
                    description = description.ifBlank { "ערוץ שנבחר ומאושר לצפייה מסוננת." }
                )

                _statusMessage.value = "הערוץ '$officialName' נוסף בהצלחה לרשימה המאושרת!"
                // Clear input text fields
                inputChannelId.value = ""
                inputCustomName.value = ""
                inputDescription.value = ""
            } catch (e: Exception) {
                _statusMessage.value = "שגיאה בעת חיבור לערוץ: ${e.localizedMessage}"
            }
        }
    }

    fun deleteChannel(channel: ApprovedChannel) {
        viewModelScope.launch {
            repository.removeChannel(channel)
            _statusMessage.value = "הערוץ '${channel.customName}' הוסר בהצלחה מהרשימה המאושרת."
        }
    }

    fun changeSupervisorPin() {
        val newPin = inputNewPin.value.trim()
        val confirm = inputPinConfirm.value.trim()

        if (newPin.length < 4) {
            _statusMessage.value = "שגיאה: קוד המפקח חייב להכיל לפחות 4 ספרות/תווים."
            return
        }

        if (newPin != confirm) {
            _statusMessage.value = "שגיאה: הקודים החדשים אינם תואמים זה לזה."
            return
        }

        viewModelScope.launch {
            repository.updateSupervisorPin(newPin)
            _statusMessage.value = "קוד המפקח עודכן ונשמר בהצלחה!"
            inputNewPin.value = ""
            inputPinConfirm.value = ""
        }
    }
}
