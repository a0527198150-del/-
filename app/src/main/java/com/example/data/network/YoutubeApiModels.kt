package com.example.data.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YtThumbnail(
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class YtThumbnails(
    val default: YtThumbnail? = null,
    val medium: YtThumbnail? = null,
    val high: YtThumbnail? = null
)

// ---- channels.list ----

@JsonClass(generateAdapter = true)
data class YtChannelListResponse(
    val items: List<YtChannelItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YtChannelItem(
    val id: String? = null,
    val snippet: YtChannelSnippet? = null
)

@JsonClass(generateAdapter = true)
data class YtChannelSnippet(
    val title: String? = null,
    val description: String? = null,
    val thumbnails: YtThumbnails? = null
)

// ---- playlistItems.list ----

@JsonClass(generateAdapter = true)
data class YtPlaylistItemListResponse(
    val items: List<YtPlaylistItem> = emptyList(),
    val nextPageToken: String? = null
)

@JsonClass(generateAdapter = true)
data class YtPlaylistItem(
    val snippet: YtPlaylistItemSnippet? = null
)

@JsonClass(generateAdapter = true)
data class YtPlaylistItemSnippet(
    val title: String? = null,
    val description: String? = null,
    val channelTitle: String? = null,
    val publishedAt: String? = null,
    val thumbnails: YtThumbnails? = null,
    val resourceId: YtResourceId? = null
)

@JsonClass(generateAdapter = true)
data class YtResourceId(
    val videoId: String? = null
)

// ---- search.list ----

@JsonClass(generateAdapter = true)
data class YtSearchListResponse(
    val items: List<YtSearchItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YtSearchItem(
    val id: YtSearchId? = null
)

@JsonClass(generateAdapter = true)
data class YtSearchId(
    val channelId: String? = null
)
