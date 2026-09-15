package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryAddCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.data.database.ApprovedChannel
import com.example.data.network.YoutubeVideo
import com.example.data.settings.WatchProgress
import com.example.ui.components.ChannelChip
import com.example.ui.components.ChannelRow
import com.example.ui.components.ContinueWatchingTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.ShimmerRow
import com.example.ui.components.StatusPanel
import com.example.ui.components.VideoRow
import com.example.ui.theme.Accent
import com.example.ui.theme.Ink
import com.example.ui.theme.Ivory
import com.example.ui.theme.Line

/** The "what's new" feed: continue watching, channel strip, newest lessons. */
@Composable
fun FeedTab(
    feedState: VideosUiState,
    channels: List<ApprovedChannel>,
    continueWatching: List<WatchProgress>,
    onVideoClick: (YoutubeVideo) -> Unit,
    onResume: (WatchProgress) -> Unit,
    onChannelClick: (ApprovedChannel) -> Unit,
    onRefresh: () -> Unit,
    onOpenSupervisor: () -> Unit,
) {
    if (channels.isEmpty()) {
        StatusPanel(
            icon = Icons.Filled.LibraryAddCheck,
            title = "אין עדיין ערוצים מאושרים",
            message = "היכנס למצב מפקח והוסף את הערוצים שמותר לצפות בהם.",
            action = {
                Button(
                    onClick = onOpenSupervisor,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent.Primary),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("כניסה למצב מפקח", style = MaterialTheme.typography.labelLarge)
                }
            },
        )
        return
    }

    val progressByVideo = remember(continueWatching) { continueWatching.associateBy { it.videoId } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (continueWatching.isNotEmpty()) {
            item {
                SectionHeader(title = "המשך צפייה", subtitle = "חזור בדיוק לאן שהפסקת")
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(continueWatching, key = { it.videoId }) { progress ->
                        ContinueWatchingTile(progress = progress, onClick = { onResume(progress) })
                    }
                }
            }
        }

        item {
            SectionHeader(title = "הערוצים שלי", subtitle = "${channels.size} ערוצים מאושרים")
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(channels, key = { it.channelId }) { channel ->
                    ChannelChip(channel = channel, onClick = { onChannelClick(channel) })
                }
            }
        }

        item {
            SectionHeader(
                title = "השיעורים האחרונים",
                subtitle = "מתעדכן מכל הערוצים המאושרים",
                action = {
                    IconButton(onClick = onRefresh, modifier = Modifier.testTag("feed_refresh_button")) {
                        Icon(Icons.Filled.Refresh, contentDescription = "רענן", tint = Accent.PrimarySoft)
                    }
                },
            )
        }

        when (feedState) {
            is VideosUiState.Loading, is VideosUiState.Idle -> {
                items(4) { ShimmerRow() }
            }

            is VideosUiState.Error -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Ink.Surface)
                            .border(1.dp, Line.Soft, RoundedCornerShape(18.dp))
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(Icons.Filled.WifiOff, contentDescription = null, tint = Accent.Danger, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.height(10.dp))
                        Text(feedState.message, style = MaterialTheme.typography.bodyMedium, color = Ivory.Muted)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onRefresh,
                            colors = ButtonDefaults.buttonColors(containerColor = Accent.Primary),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("נסה שוב", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            is VideosUiState.Success -> {
                items(feedState.videos, key = { it.id }) { video ->
                    VideoRow(
                        video = video,
                        onClick = { onVideoClick(video) },
                        progressFraction = progressByVideo[video.id]?.fraction ?: 0f,
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelsTab(
    channels: List<ApprovedChannel>,
    onChannelClick: (ApprovedChannel) -> Unit,
    onOpenSupervisor: () -> Unit,
) {
    if (channels.isEmpty()) {
        StatusPanel(
            icon = Icons.Filled.LibraryAddCheck,
            title = "הרשימה ריקה",
            message = "מצב מפקח מאפשר להוסיף ערוצים לפי קישור, שם משתמש או מזהה.",
            action = {
                Button(
                    onClick = onOpenSupervisor,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent.Primary),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("הוספת ערוץ", style = MaterialTheme.typography.labelLarge)
                }
            },
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionHeader(title = "ערוצים מאושרים", subtitle = "${channels.size} ערוצים זמינים לצפייה")
        }
        items(channels, key = { it.channelId }) { channel ->
            ChannelRow(channel = channel, onClick = { onChannelClick(channel) })
        }
    }
}

/** Searches only inside the already approved feed — never YouTube at large. */
@Composable
fun SearchTab(
    query: String,
    feedState: VideosUiState,
    onQueryChange: (String) -> Unit,
    onVideoClick: (YoutubeVideo) -> Unit,
) {
    val allVideos = (feedState as? VideosUiState.Success)?.videos.orEmpty()
    val results = remember(query, allVideos) {
        if (query.isBlank()) emptyList() else allVideos.filter { video ->
            video.title.contains(query, ignoreCase = true) ||
                video.description.contains(query, ignoreCase = true) ||
                video.channelName.contains(query, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            placeholder = "חפש שיעור בכל הערוצים המאושרים...",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("global_search_input"),
        )

        when {
            query.isBlank() -> StatusPanel(
                icon = Icons.Filled.Search,
                title = "חיפוש בתוך הסינון",
                message = "התוצאות מוגבלות לשיעורים מהערוצים שאושרו בלבד.",
            )

            results.isEmpty() -> StatusPanel(
                icon = Icons.Filled.SearchOff,
                title = "לא נמצאו תוצאות",
                message = "נסה מילות חיפוש אחרות.",
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(results, key = { it.id }) { video ->
                    VideoRow(video = video, onClick = { onVideoClick(video) })
                }
            }
        }
    }
}

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = Ivory.Faint) },
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null, tint = Ivory.Faint, modifier = Modifier.size(20.dp))
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "נקה", tint = Ivory.Faint, modifier = Modifier.size(18.dp))
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Accent.Primary,
            unfocusedBorderColor = Line.Soft,
            focusedContainerColor = Ink.Surface,
            unfocusedContainerColor = Ink.Surface,
            focusedTextColor = Ivory.Strong,
            unfocusedTextColor = Ivory.Strong,
            cursorColor = Accent.PrimarySoft,
        ),
        modifier = modifier,
    )
}

/** Channel detail: hero header with the avatar, then the channel's lessons. */
@Composable
fun ChannelDetailScreen(
    channel: ApprovedChannel,
    videosState: VideosUiState,
    continueWatching: List<WatchProgress>,
    onVideoClick: (YoutubeVideo) -> Unit,
    onRefresh: () -> Unit,
) {
    val localQuery = remember { androidx.compose.runtime.mutableStateOf("") }
    val progressByVideo = remember(continueWatching) { continueWatching.associateBy { it.videoId } }

    val videos = (videosState as? VideosUiState.Success)?.videos.orEmpty()
    val filtered = remember(videos, localQuery.value) {
        if (localQuery.value.isBlank()) videos else videos.filter {
            it.title.contains(localQuery.value, ignoreCase = true) ||
                it.description.contains(localQuery.value, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                com.example.ui.components.ChannelAvatar(channel, size = 64.dp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(channel.customName, style = MaterialTheme.typography.headlineSmall, color = Ivory.Strong)
                    if (channel.officialName.isNotBlank() && channel.officialName != channel.customName) {
                        Text(channel.officialName, style = MaterialTheme.typography.labelSmall, color = Ivory.Faint)
                    }
                }
                IconButton(onClick = onRefresh, modifier = Modifier.testTag("refresh_channel_videos_button")) {
                    Icon(Icons.Filled.Refresh, contentDescription = "רענן", tint = Accent.PrimarySoft)
                }
            }
        }

        if (channel.description.isNotBlank()) {
            item {
                Text(channel.description, style = MaterialTheme.typography.bodySmall, color = Ivory.Muted)
            }
        }

        item {
            SearchField(
                query = localQuery.value,
                onQueryChange = { localQuery.value = it },
                placeholder = "חפש שיעור בערוץ...",
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("channel_video_search_input"),
            )
        }

        when (videosState) {
            is VideosUiState.Loading, is VideosUiState.Idle -> items(5) { ShimmerRow() }

            is VideosUiState.Error -> item {
                Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                    StatusPanel(
                        icon = Icons.Filled.WifiOff,
                        title = "לא ניתן לטעון את הערוץ",
                        message = videosState.message,
                        accent = Accent.Danger,
                        action = {
                            Button(
                                onClick = onRefresh,
                                colors = ButtonDefaults.buttonColors(containerColor = Accent.Primary),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("נסה שוב", style = MaterialTheme.typography.labelLarge)
                            }
                        },
                    )
                }
            }

            is VideosUiState.Success -> {
                if (filtered.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                            StatusPanel(
                                icon = Icons.Filled.SearchOff,
                                title = "אין תוצאות",
                                message = "לא נמצאו שיעורים התואמים לחיפוש.",
                            )
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { video ->
                        VideoRow(
                            video = video,
                            onClick = { onVideoClick(video) },
                            showChannel = false,
                            progressFraction = progressByVideo[video.id]?.fraction ?: 0f,
                        )
                    }
                }
            }
        }
    }
}
