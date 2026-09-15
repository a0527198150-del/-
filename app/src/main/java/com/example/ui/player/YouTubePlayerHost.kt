package com.example.ui.player

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

/**
 * Observable state of the embedded player, driven by the library's callbacks.
 * The composable UI reads this; nothing here touches YouTube's own web chrome.
 */
@Stable
class KosherPlayerState {
    internal var player: YouTubePlayer? = null

    var isReady: Boolean by mutableStateOf(false)
        internal set
    var playerState: PlayerConstants.PlayerState by mutableStateOf(PlayerConstants.PlayerState.UNKNOWN)
        internal set
    var currentSecond: Float by mutableFloatStateOf(0f)
        internal set
    var duration: Float by mutableFloatStateOf(0f)
        internal set
    var bufferedFraction: Float by mutableFloatStateOf(0f)
        internal set
    var error: PlayerConstants.PlayerError? by mutableStateOf<PlayerConstants.PlayerError?>(null)
        internal set
    var speed: PlayerConstants.PlaybackRate by mutableStateOf(PlayerConstants.PlaybackRate.RATE_1)
        internal set

    /** True while the user drags the scrubber, so callbacks don't fight the gesture. */
    var isScrubbing: Boolean by mutableStateOf(false)
    var scrubPosition: Float by mutableFloatStateOf(0f)

    val isPlaying: Boolean
        get() = playerState == PlayerConstants.PlayerState.PLAYING

    val isBuffering: Boolean
        get() = playerState == PlayerConstants.PlayerState.BUFFERING ||
            (!isReady && error == null)

    val hasEnded: Boolean
        get() = playerState == PlayerConstants.PlayerState.ENDED

    val displayPosition: Float
        get() = if (isScrubbing) scrubPosition else currentSecond

    fun play() {
        player?.play()
    }

    fun pause() {
        player?.pause()
    }

    fun togglePlayPause() {
        if (isPlaying) pause() else play()
    }

    fun seekTo(seconds: Float) {
        val target = seconds.coerceIn(0f, if (duration > 0f) duration else Float.MAX_VALUE)
        currentSecond = target
        player?.seekTo(target)
    }

    fun seekBy(deltaSeconds: Float) = seekTo(currentSecond + deltaSeconds)

    fun setSpeed(rate: PlayerConstants.PlaybackRate) {
        speed = rate
        player?.setPlaybackRate(rate)
    }
}

@Composable
fun rememberKosherPlayerState(): KosherPlayerState = remember { KosherPlayerState() }

/**
 * Hosts the native `YouTubePlayerView` (IFrame player inside a hardware
 * accelerated WebView managed by the library) and wires its callbacks into
 * [state]. The web UI is switched off entirely — controls are drawn in Compose.
 *
 * Keep this composable at a stable position in the tree: moving it between
 * parents recreates the view and restarts playback.
 */
@Composable
fun KosherYouTubePlayer(
    videoId: String,
    state: KosherPlayerState,
    modifier: Modifier = Modifier,
    startSeconds: Float = 0f,
    autoplay: Boolean = true,
    onPositionChanged: (position: Float, duration: Float) -> Unit = { _, _ -> },
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val positionListener by rememberUpdatedState(onPositionChanged)
    val playerView = remember { PlayerViewHolder() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val view = android.view.LayoutInflater
                .from(context)
                .inflate(R.layout.view_youtube_player, null, false) as YouTubePlayerView
            view.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )

            val options = IFramePlayerOptions.Builder()
                // 0 = no web UI at all: no YouTube logo, no share, no "watch on
                // YouTube", no end-screen suggestions. Controls are ours.
                .controls(0)
                .fullscreen(0)
                .rel(0)
                .ivLoadPolicy(3)
                .modestBranding(1)
                .ccLoadPolicy(0)
                .langPref("he")
                .build()

            view.initialize(
                object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        state.player = youTubePlayer
                        state.isReady = true
                        state.error = null
                        val id = playerView.pendingVideoId ?: videoId
                        val start = playerView.pendingStart ?: startSeconds
                        if (autoplay) {
                            youTubePlayer.loadVideo(id, start)
                        } else {
                            youTubePlayer.cueVideo(id, start)
                        }
                        playerView.loadedVideoId = id
                    }

                    override fun onStateChange(
                        youTubePlayer: YouTubePlayer,
                        playerState: PlayerConstants.PlayerState,
                    ) {
                        state.playerState = playerState
                    }

                    override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                        if (!state.isScrubbing) state.currentSecond = second
                        positionListener(second, state.duration)
                    }

                    override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                        state.duration = duration
                    }

                    override fun onVideoLoadedFraction(youTubePlayer: YouTubePlayer, loadedFraction: Float) {
                        state.bufferedFraction = loadedFraction
                    }

                    override fun onPlaybackRateChange(
                        youTubePlayer: YouTubePlayer,
                        playbackRate: PlayerConstants.PlaybackRate,
                    ) {
                        state.speed = playbackRate
                    }

                    override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                        state.error = error
                    }
                },
                options,
            )

            playerView.view = view
            // The view knows how to pause on ON_STOP and tear down on ON_DESTROY.
            lifecycle.addObserver(view)
            view
        },
        onRelease = { view ->
            state.player = null
            state.isReady = false
            playerView.view = null
            lifecycle.removeObserver(view)
            view.release()
        },
    )

    // Swap the video without tearing the player down.
    LaunchedEffect(videoId, startSeconds) {
        playerView.pendingVideoId = videoId
        playerView.pendingStart = startSeconds
        val player = state.player
        if (player != null && playerView.loadedVideoId != videoId) {
            state.currentSecond = startSeconds
            state.duration = 0f
            state.bufferedFraction = 0f
            state.error = null
            if (autoplay) player.loadVideo(videoId, startSeconds) else player.cueVideo(videoId, startSeconds)
            playerView.loadedVideoId = videoId
        }
    }

}

/** Mutable box so the factory lambda and the effects can share the inflated view. */
private class PlayerViewHolder {
    var view: YouTubePlayerView? = null
    var loadedVideoId: String? = null
    var pendingVideoId: String? = null
    var pendingStart: Float? = null
}
