package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.database.ApprovedChannel
import com.example.data.network.YoutubeTimeFormat
import com.example.data.network.YoutubeVideo
import com.example.data.settings.WatchProgress
import com.example.ui.theme.Accent
import com.example.ui.theme.Gradients
import com.example.ui.theme.Ink
import com.example.ui.theme.Ivory
import com.example.ui.theme.Line

/** Thumbnail with the duration badge and optional resume bar, shared by every card. */
@Composable
private fun Thumbnail(
    imageUrl: String,
    modifier: Modifier = Modifier,
    durationSeconds: Int = 0,
    progressFraction: Float = 0f,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Ink.Base),
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(modifier = Modifier.fillMaxSize().background(Gradients.ScrimBottom))

        if (durationSeconds > 0) {
            Pill(
                text = YoutubeTimeFormat.formatClock(durationSeconds),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp),
            )
        }
        if (progressFraction > 0f) {
            WatchProgressBar(
                fraction = progressFraction,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        overlay()
    }
}

/** Full width video row used in the channel and search lists. */
@Composable
fun VideoRow(
    video: YoutubeVideo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progressFraction: Float = 0f,
    showChannel: Boolean = true,
) {
    SurfaceCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("video_item_${video.id}"),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Thumbnail(
                imageUrl = video.highResThumbnail,
                durationSeconds = video.durationSeconds,
                progressFraction = progressFraction,
                modifier = Modifier
                    .width(136.dp)
                    .aspectRatio(16f / 9f),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Ivory.Strong,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = listOfNotNull(
                        video.channelName.takeIf { showChannel && it.isNotBlank() },
                        video.publishedAt.takeIf { it.isNotBlank() },
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Ivory.Faint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Compact card for the horizontal carousels on the home screen. */
@Composable
fun VideoTile(
    video: YoutubeVideo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progressFraction: Float = 0f,
) {
    Column(
        modifier = modifier
            .width(230.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("video_tile_${video.id}"),
    ) {
        Thumbnail(
            imageUrl = video.highResThumbnail,
            durationSeconds = video.durationSeconds,
            progressFraction = progressFraction,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = video.title,
            style = MaterialTheme.typography.bodyMedium,
            color = Ivory.Strong,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = listOfNotNull(
                video.channelName.takeIf { it.isNotBlank() },
                video.publishedAt.takeIf { it.isNotBlank() },
            ).joinToString(" • "),
            style = MaterialTheme.typography.labelSmall,
            color = Ivory.Faint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ContinueWatchingTile(
    progress: WatchProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(210.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
    ) {
        Thumbnail(
            imageUrl = progress.thumbnailUrl,
            progressFraction = progress.fraction,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        ) {
            Pill(
                text = "נותרו ${YoutubeTimeFormat.formatClock((progress.durationSeconds - progress.positionSeconds).toInt())}",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
                background = Accent.Gold,
                contentColor = Ink.Base,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = progress.title,
            style = MaterialTheme.typography.bodyMedium,
            color = Ivory.Strong,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Circular channel avatar, falling back to the first letter of the name. */
@Composable
fun ChannelAvatar(
    channel: ApprovedChannel,
    size: androidx.compose.ui.unit.Dp = 52.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Gradients.Royal)
            .border(1.dp, Line.Strong, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (channel.avatarUrl.isNotBlank()) {
            AsyncImage(
                model = channel.avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        } else {
            Text(
                text = channel.customName.trim().take(1),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }
    }
}

@Composable
fun ChannelRow(
    channel: ApprovedChannel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("channel_card_${channel.channelId}"),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ChannelAvatar(channel)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.customName,
                    style = MaterialTheme.typography.titleSmall,
                    color = Ivory.Strong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = channel.description.ifBlank { "ערוץ שאושר לצפייה מסוננת" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Ivory.Faint,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = null,
                tint = Accent.PrimarySoft,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** Avatar + name, used for the horizontal channel strip on the home screen. */
@Composable
fun ChannelChip(
    channel: ApprovedChannel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(84.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ChannelAvatar(channel, size = 62.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = channel.customName,
            style = MaterialTheme.typography.labelSmall,
            color = Ivory.Muted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
