package com.example.data.network

data class YoutubeVideo(
    val id: String,
    val title: String,
    val channelId: String,
    val channelName: String,
    val thumbnailUrl: String,
    val description: String,
    /** Human readable publish date, already localised for display. */
    val publishedAt: String,
    /** Epoch millis when known, used for sorting mixed feeds. 0 when unknown. */
    val publishedAtMillis: Long = 0L,
    /** Runtime in seconds when known, 0 otherwise. */
    val durationSeconds: Int = 0,
) {
    val highResThumbnail: String
        get() = if (thumbnailUrl.isBlank()) "https://i.ytimg.com/vi/$id/hqdefault.jpg" else thumbnailUrl
}

/** Channel metadata fetched from YouTube, used to make the channel cards look real. */
data class YoutubeChannelInfo(
    val channelId: String,
    val title: String,
    val description: String,
    val avatarUrl: String,
)
