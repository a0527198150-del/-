package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.KosherBadge
import com.example.ui.theme.Accent
import com.example.ui.theme.Gradients
import com.example.ui.theme.Ink
import com.example.ui.theme.Ivory
import com.example.ui.theme.Line

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val launchState by viewModel.launchState.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val keyCheck by viewModel.keyCheck.collectAsStateWithLifecycle()

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStatusMessage()
        }
    }

    when (launchState) {
        LaunchState.Loading -> SplashScreen(modifier)

        LaunchState.NeedsSetup -> OnboardingScreen(
            keyCheck = keyCheck,
            onSubmit = { key, verify -> viewModel.saveApiKeyAndContinue(key, verify) },
            onSkip = { viewModel.skipApiKey() },
            onKeyEdited = { viewModel.resetKeyCheck() },
        )

        LaunchState.Ready -> AppShell(viewModel = viewModel, keyCheck = keyCheck, modifier = modifier)
    }
}

@Composable
private fun SplashScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Gradients.Hero),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(74.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Gradients.GoldSheen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Shield, contentDescription = null, tint = Ink.Base, modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("ערוצים כשרים", style = MaterialTheme.typography.headlineSmall, color = Ivory.Strong)
        Spacer(Modifier.height(14.dp))
        CircularProgressIndicator(color = Accent.Gold, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppShell(viewModel: MainViewModel, keyCheck: KeyCheckState, modifier: Modifier = Modifier) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val homeTab by viewModel.homeTab.collectAsStateWithLifecycle()
    val channels by viewModel.approvedChannels.collectAsStateWithLifecycle()
    val continueWatching by viewModel.continueWatching.collectAsStateWithLifecycle()
    val feedState by viewModel.feedState.collectAsStateWithLifecycle()
    val channelVideos by viewModel.channelVideos.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isSupervisor by viewModel.isSupervisor.collectAsStateWithLifecycle()
    val showPinPrompt by viewModel.showPinPrompt.collectAsStateWithLifecycle()
    val pinError by viewModel.pinError.collectAsStateWithLifecycle()
    val isAddingChannel by viewModel.isAddingChannel.collectAsStateWithLifecycle()

    val channelInput by viewModel.inputChannelId.collectAsStateWithLifecycle()
    val nameInput by viewModel.inputCustomName.collectAsStateWithLifecycle()
    val descriptionInput by viewModel.inputDescription.collectAsStateWithLifecycle()
    val newPin by viewModel.inputNewPin.collectAsStateWithLifecycle()
    val confirmPin by viewModel.inputPinConfirm.collectAsStateWithLifecycle()

    // The player owns its own back handling (fullscreen first).
    BackHandler(enabled = destination !is Destination.Home && destination !is Destination.Player) {
        viewModel.navigateBack()
    }

    val isPlayer = destination is Destination.Player

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Ink.Base,
        topBar = {
            if (!isPlayer) {
                TopAppBar(
                    title = { TopBarTitle(destination = destination, homeTab = homeTab) },
                    navigationIcon = {
                        if (destination !is Destination.Home) {
                            IconButton(
                                onClick = { viewModel.navigateBack() },
                                modifier = Modifier.testTag("back_to_channels_button"),
                            ) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "חזרה", tint = Ivory.Strong)
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.openSupervisor() },
                            modifier = Modifier.testTag("supervisor_toggle_button"),
                        ) {
                            Icon(
                                imageVector = if (isSupervisor) Icons.Filled.LockOpen else Icons.Filled.Lock,
                                contentDescription = if (isSupervisor) "מצב מפקח פעיל" else "כניסה למצב מפקח",
                                tint = if (isSupervisor) Accent.Gold else Ivory.Faint,
                            )
                        }
                        if (isSupervisor) {
                            IconButton(
                                onClick = { viewModel.logoutSupervisor() },
                                modifier = Modifier.testTag("supervisor_logout"),
                            ) {
                                Icon(Icons.Filled.Logout, contentDescription = "יציאה ממצב מפקח", tint = Accent.Danger)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Ink.Elevated,
                        titleContentColor = Ivory.Strong,
                    ),
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = destination is Destination.Home,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                BottomBar(selected = homeTab, onSelect = { viewModel.selectTab(it) })
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isPlayer) androidx.compose.foundation.layout.PaddingValues(0.dp) else padding),
        ) {
            when (val current = destination) {
                Destination.Home -> when (homeTab) {
                    HomeTab.FEED -> FeedTab(
                        feedState = feedState,
                        channels = channels,
                        continueWatching = continueWatching,
                        onVideoClick = { viewModel.playVideo(it) },
                        onResume = { viewModel.resumeWatching(it) },
                        onChannelClick = { viewModel.openChannel(it) },
                        onRefresh = { viewModel.refreshFeed() },
                        onOpenSupervisor = { viewModel.openSupervisor() },
                    )

                    HomeTab.CHANNELS -> ChannelsTab(
                        channels = channels,
                        onChannelClick = { viewModel.openChannel(it) },
                        onOpenSupervisor = { viewModel.openSupervisor() },
                    )

                    HomeTab.SEARCH -> SearchTab(
                        query = searchQuery,
                        feedState = feedState,
                        onQueryChange = { viewModel.setSearchQuery(it) },
                        onVideoClick = { viewModel.playVideo(it) },
                    )
                }

                is Destination.ChannelDetail -> ChannelDetailScreen(
                    channel = current.channel,
                    videosState = channelVideos,
                    continueWatching = continueWatching,
                    onVideoClick = { viewModel.playVideo(it) },
                    onRefresh = { viewModel.loadChannelVideos(current.channel.channelId) },
                )

                is Destination.Player -> {
                    val channelVideoList = (channelVideos as? VideosUiState.Success)?.videos.orEmpty()
                    val feedVideoList = (feedState as? VideosUiState.Success)?.videos.orEmpty()
                    val pool = channelVideoList.ifEmpty { feedVideoList }
                    PlayerScreen(
                        video = current.video,
                        startSeconds = current.startSeconds,
                        channel = channels.firstOrNull { it.channelId == current.video.channelId },
                        upNext = pool.filter { it.id != current.video.id }.take(12),
                        autoplay = settings.autoplay,
                        onBack = { if (!viewModel.navigateBack()) viewModel.goHome() },
                        onPlayVideo = { viewModel.playVideo(it) },
                        onProgress = { video, position, duration ->
                            viewModel.saveProgress(video, position, duration)
                        },
                    )
                }

                Destination.Supervisor -> SupervisorScreen(
                    settings = settings,
                    channels = channels,
                    keyCheck = keyCheck,
                    isAddingChannel = isAddingChannel,
                    channelInput = channelInput,
                    nameInput = nameInput,
                    descriptionInput = descriptionInput,
                    newPin = newPin,
                    confirmPin = confirmPin,
                    onChannelInputChange = { viewModel.inputChannelId.value = it },
                    onNameInputChange = { viewModel.inputCustomName.value = it },
                    onDescriptionInputChange = { viewModel.inputDescription.value = it },
                    onNewPinChange = { viewModel.inputNewPin.value = it },
                    onConfirmPinChange = { viewModel.inputPinConfirm.value = it },
                    onAddChannel = { viewModel.addNewApprovedChannel() },
                    onDeleteChannel = { viewModel.deleteChannel(it) },
                    onSaveApiKey = { viewModel.updateApiKey(it) },
                    onRefreshMetadata = { viewModel.refreshChannelMetadata() },
                    onChangePin = { viewModel.changeSupervisorPin() },
                    onClearHistory = { viewModel.clearWatchHistory() },
                )
            }

            if (showPinPrompt) {
                PinPromptDialog(
                    errorMessage = pinError,
                    onDismiss = { viewModel.closePinPrompt() },
                    onSubmit = { viewModel.authenticateSupervisor(it) },
                )
            }
        }
    }
}

@Composable
private fun TopBarTitle(destination: Destination, homeTab: HomeTab) {
    val title = when (destination) {
        Destination.Home -> when (homeTab) {
            HomeTab.FEED -> "ערוצים כשרים"
            HomeTab.CHANNELS -> "הערוצים שלי"
            HomeTab.SEARCH -> "חיפוש מסונן"
        }

        is Destination.ChannelDetail -> destination.channel.customName
        is Destination.Player -> destination.video.title
        Destination.Supervisor -> "מצב מפקח"
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Gradients.GoldSheen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Shield, contentDescription = null, tint = Ink.Base, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Ivory.Strong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "סביבה מוגנת ומבוקרת",
                style = MaterialTheme.typography.labelSmall,
                color = Ivory.Faint,
            )
        }
    }
}

@Composable
private fun BottomBar(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ink.Elevated)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BottomBarItem(Icons.Filled.Home, "בית", selected == HomeTab.FEED) { onSelect(HomeTab.FEED) }
        BottomBarItem(Icons.Filled.Subscriptions, "ערוצים", selected == HomeTab.CHANNELS) { onSelect(HomeTab.CHANNELS) }
        BottomBarItem(Icons.Filled.Search, "חיפוש", selected == HomeTab.SEARCH) { onSelect(HomeTab.SEARCH) }
    }
}

@Composable
private fun BottomBarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (isSelected) Accent.Gold else Ivory.Faint
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .testTag("tab_$label"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(width = 18.dp, height = 2.dp)
                .clip(CircleShape)
                .background(if (isSelected) Accent.Gold else Color.Transparent),
        )
    }
}
