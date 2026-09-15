package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.database.ApprovedChannel
import com.example.data.network.YoutubeTimeFormat
import com.example.data.network.YoutubeVideo
import com.example.ui.components.ChannelAvatar
import com.example.ui.components.KosherBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.SurfaceCard
import com.example.ui.components.VideoRow
import com.example.ui.player.KosherYouTubePlayer
import com.example.ui.player.rememberKosherPlayerState
import com.example.ui.theme.Accent
import com.example.ui.theme.Gradients
import com.example.ui.theme.Ink
import com.example.ui.theme.Ivory
import com.example.ui.theme.Line
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import kotlinx.coroutines.delay

private val SPEEDS = listOf(
    PlayerConstants.PlaybackRate.RATE_0_5,
    PlayerConstants.PlaybackRate.RATE_1,
    PlayerConstants.PlaybackRate.RATE_1_5,
    PlayerConstants.PlaybackRate.RATE_2,
)

private fun PlayerConstants.PlaybackRate.label(): String = when (this) {
    PlayerConstants.PlaybackRate.RATE_0_25 -> "0.25x"
    PlayerConstants.PlaybackRate.RATE_0_5 -> "0.5x"
    PlayerConstants.PlaybackRate.RATE_1_5 -> "1.5x"
    PlayerConstants.PlaybackRate.RATE_2 -> "2x"
    else -> "1x"
}

fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun PlayerScreen(
    video: YoutubeVideo,
    startSeconds: Float,
    channel: ApprovedChannel?,
    upNext: List<YoutubeVideo>,
    autoplay: Boolean,
    onBack: () -> Unit,
    onPlayVideo: (YoutubeVideo) -> Unit,
    onProgress: (video: YoutubeVideo, position: Float, duration: Float) -> Unit,
) {
    val state = rememberKosherPlayerState()
    val context = LocalContext.current
    val view = LocalView.current
    var isFullScreen by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var lastSavedSecond by remember { mutableIntStateOf(0) }
    // Bumping this key forces the player to re-cue after a playback error.
    var reloadToken by remember { mutableIntStateOf(0) }

    // Screen should not sleep mid-lesson.
    LaunchedEffect(state.isPlaying) {
        view.keepScreenOn = state.isPlaying
    }
    DisposableEffect(Unit) {
        onDispose { view.keepScreenOn = false }
    }

    // Auto-hide the controls while playing.
    LaunchedEffect(controlsVisible, state.isPlaying, state.isScrubbing) {
        if (controlsVisible && state.isPlaying && !state.isScrubbing) {
            delay(3_500)
            controlsVisible = false
        }
    }

    // Apply orientation + system bars for fullscreen, and always restore on exit.
    val activity = remember(context) { context.findActivity() }
    LaunchedEffect(isFullScreen, activity) {
        val current = activity ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(current.window, view)
        if (isFullScreen) {
            current.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            current.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
            controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }

    // Leaving the player must never strand the app in landscape with hidden bars.
    DisposableEffect(activity) {
        onDispose {
            val current = activity ?: return@onDispose
            current.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
            WindowCompat.getInsetsController(current.window, view)
                .show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }

    BackHandler(enabled = true) {
        if (isFullScreen) isFullScreen = false else onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.Base),
    ) {
        // The player Box keeps the same position in the tree in both layouts, so
        // toggling fullscreen never recreates the view or restarts playback.
        Box(
            modifier = if (isFullScreen) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .aspectRatio(16f / 9f)
            }.background(Color.Black),
        ) {
            key(reloadToken) {
                KosherYouTubePlayer(
                    videoId = video.id,
                    state = state,
                    startSeconds = startSeconds,
                    autoplay = autoplay,
                    modifier = Modifier.fillMaxSize(),
                    onPositionChanged = { position, duration ->
                        val second = position.toInt()
                        if (duration > 0f && second != lastSavedSecond && second % 5 == 0) {
                            lastSavedSecond = second
                            onProgress(video, position, duration)
                        }
                    },
                )
            }

            PlayerControls(
                state = state,
                title = video.title,
                visible = controlsVisible,
                isFullScreen = isFullScreen,
                onToggleControls = { controlsVisible = !controlsVisible },
                onToggleFullScreen = { isFullScreen = !isFullScreen },
                onBack = { if (isFullScreen) isFullScreen = false else onBack() },
                onRetry = { reloadToken++ },
            )
        }

        if (!isFullScreen) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Ivory.Strong,
                    )
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (channel != null) {
                            ChannelAvatar(channel, size = 40.dp)
                            Spacer(Modifier.width(10.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = channel?.customName ?: video.channelName,
                                style = MaterialTheme.typography.titleSmall,
                                color = Ivory.Strong,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (video.publishedAt.isNotBlank()) {
                                Text(
                                    text = video.publishedAt,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Ivory.Faint,
                                )
                            }
                        }
                        KosherBadge("צפייה מסוננת")
                    }
                }

                if (video.description.isNotBlank()) {
                    item { DescriptionCard(video.description) }
                }

                if (upNext.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "המשך השיעורים בערוץ",
                            subtitle = "${upNext.size} שיעורים נוספים",
                        )
                    }
                    items(upNext, key = { it.id }) { next ->
                        VideoRow(video = next, onClick = { onPlayVideo(next) }, showChannel = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun DescriptionCard(description: String) {
    var expanded by remember { mutableStateOf(false) }
    SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded },
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("על השיעור", style = MaterialTheme.typography.titleSmall, color = Ivory.Strong)
            Spacer(Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Ivory.Muted,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (expanded) "הצג פחות" else "הצג עוד",
                style = MaterialTheme.typography.labelSmall,
                color = Accent.PrimarySoft,
            )
        }
    }
}

/**
 * Every control is drawn here in Compose. YouTube's own web UI is disabled, so
 * there is no way to reach search, comments, related videos or the YouTube app.
 */
@Composable
private fun PlayerControls(
    state: com.example.ui.player.KosherPlayerState,
    title: String,
    visible: Boolean,
    isFullScreen: Boolean,
    onToggleControls: () -> Unit,
    onToggleFullScreen: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggleControls,
            ),
    ) {
        if (state.error != null) {
            PlayerErrorPanel(error = state.error, onRetry = onRetry, onBack = onBack)
            return@Box
        }

        if (state.isBuffering && !state.isScrubbing) {
            CircularProgressIndicator(
                color = Accent.Gold,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(46.dp),
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top bar: back + title, only worth the space in fullscreen.
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(Gradients.ScrimTop)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ControlButton(
                        icon = Icons.Filled.ArrowBack,
                        contentDescription = "חזרה",
                        onClick = onBack,
                        modifier = Modifier.testTag("player_back_button"),
                    )
                    if (isFullScreen) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = Ivory.Strong,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Centre transport controls.
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(26.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ControlButton(
                        icon = Icons.Filled.Replay10,
                        contentDescription = "אחורה 10 שניות",
                        onClick = { state.seekBy(-10f) },
                        size = 46.dp,
                    )
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Accent.Primary)
                            .clickable {
                                if (state.hasEnded) {
                                    state.seekTo(0f)
                                    state.play()
                                } else {
                                    state.togglePlayPause()
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = when {
                                state.hasEnded -> Icons.Filled.Replay
                                state.isPlaying -> Icons.Filled.Pause
                                else -> Icons.Filled.PlayArrow
                            },
                            contentDescription = if (state.isPlaying) "השהה" else "נגן",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    ControlButton(
                        icon = Icons.Filled.Forward10,
                        contentDescription = "קדימה 10 שניות",
                        onClick = { state.seekBy(10f) },
                        size = 46.dp,
                    )
                }

                // Scrubber + time + speed + fullscreen.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Gradients.ScrimBottom)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Slider(
                            value = state.displayPosition.coerceIn(0f, state.duration.coerceAtLeast(1f)),
                            onValueChange = {
                                state.isScrubbing = true
                                state.scrubPosition = it
                            },
                            onValueChangeFinished = {
                                state.seekTo(state.scrubPosition)
                                state.isScrubbing = false
                            },
                            valueRange = 0f..state.duration.coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Accent.Gold,
                                activeTrackColor = Accent.Gold,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .testTag("player_seek_bar"),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = YoutubeTimeFormat.formatClock(state.displayPosition.toInt()) +
                                    " / " + YoutubeTimeFormat.formatClock(state.duration.toInt()),
                                style = MaterialTheme.typography.labelSmall,
                                color = Ivory.Strong,
                            )
                            Spacer(Modifier.weight(1f))
                            SpeedChip(state = state)
                            Spacer(Modifier.width(4.dp))
                            ControlButton(
                                icon = if (isFullScreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                contentDescription = "מסך מלא",
                                onClick = onToggleFullScreen,
                                modifier = Modifier.testTag("player_fullscreen_button"),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedChip(state: com.example.ui.player.KosherPlayerState) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable {
                val nextIndex = (SPEEDS.indexOf(state.speed).takeIf { it >= 0 }?.plus(1) ?: 1) % SPEEDS.size
                state.setSpeed(SPEEDS[nextIndex])
            }
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .testTag("player_speed_chip"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Speed, contentDescription = "מהירות ניגון", tint = Ivory.Strong, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(4.dp))
        Text(state.speed.label(), style = MaterialTheme.typography.labelSmall, color = Ivory.Strong)
    }
}

@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 38.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.42f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Ivory.Strong,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

@Composable
private fun PlayerErrorPanel(
    error: PlayerConstants.PlayerError?,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    val message = when (error) {
        PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER ->
            "בעל הערוץ חסם הטמעה של הסרטון הזה מחוץ ליוטיוב."

        PlayerConstants.PlayerError.VIDEO_NOT_FOUND -> "הסרטון הוסר או אינו זמין יותר."
        PlayerConstants.PlayerError.INVALID_PARAMETER_IN_REQUEST -> "מזהה הסרטון אינו תקין."
        PlayerConstants.PlayerError.HTML_5_PLAYER -> "שגיאה בנגן. נסה להפעיל שוב."
        else -> "לא הצלחנו לנגן את הסרטון כרגע."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.Base.copy(alpha = 0.94f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Accent.Danger, modifier = Modifier.size(42.dp))
        Spacer(Modifier.height(10.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = Ivory.Strong)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "נסה שוב",
                style = MaterialTheme.typography.labelLarge,
                color = Ivory.Strong,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Accent.Primary)
                    .clickable { onRetry() }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
            Text(
                text = "חזרה",
                style = MaterialTheme.typography.labelLarge,
                color = Ivory.Muted,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Line.Regular, RoundedCornerShape(12.dp))
                    .clickable { onBack() }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }
}
