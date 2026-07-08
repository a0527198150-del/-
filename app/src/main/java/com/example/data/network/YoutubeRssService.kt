package com.example.data.network

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Fetches channel videos and resolves channel IDs using the official
 * YouTube Data API v3 (https://developers.google.com/youtube/v3).
 *
 * The class name is kept as "YoutubeRssService" on purpose so that
 * ChannelRepository.kt does not need to change at all.
 */
class YoutubeRssService {

    private val apiKey: String = BuildConfig.YOUTUBE_API_KEY

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    private val moshi: Moshi = Moshi.Builder().build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(YoutubeApiService.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api: YoutubeApiService = retrofit.create(YoutubeApiService::class.java)

    private fun hasValidKey(): Boolean =
        apiKey.isNotBlank() && apiKey != "MY_YOUTUBE_API_KEY"

    suspend fun fetchChannelVideos(channelId: String): List<YoutubeVideo> = withContext(Dispatchers.IO) {
        if (!hasValidKey()) return@withContext emptyList()

        try {
            // Every channel's "uploads" playlist ID is the channel ID with
            // the "UC" prefix replaced by "UU" - this saves an extra API call
            // to channels.list just to look up contentDetails.
            val uploadsPlaylistId = if (channelId.startsWith("UC")) {
                "UU" + channelId.removePrefix("UC")
            } else {
                channelId
            }

            val playlistResponse = api.getPlaylistItems(
                playlistId = uploadsPlaylistId,
                apiKey = apiKey
            )

            playlistResponse.items.mapNotNull { item ->
                val snippet = item.snippet ?: return@mapNotNull null
                val videoId = snippet.resourceId?.videoId ?: return@mapNotNull null
                YoutubeVideo(
                    id = videoId,
                    title = snippet.title ?: "",
                    channelId = channelId,
                    channelName = snippet.channelTitle ?: "",
                    thumbnailUrl = snippet.thumbnails?.high?.url
                        ?: snippet.thumbnails?.medium?.url
                        ?: snippet.thumbnails?.default?.url
                        ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                    description = snippet.description ?: "",
                    publishedAt = snippet.publishedAt ?: ""
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun resolveChannelId(input: String): String? = withContext(Dispatchers.IO) {
        val trimmed = input.trim()

        // 1. Already a channel ID
        if (trimmed.startsWith("UC") && trimmed.length == 24) {
            return@withContext trimmed
        }

        // 2. A full channel URL that already contains the UC id
        val channelIdPattern = Regex("youtube\\.com/channel/(UC[a-zA-Z0-9_-]{22})")
        channelIdPattern.find(trimmed)?.groupValues?.get(1)?.let { return@withContext it }

        if (!hasValidKey()) return@withContext null

        // 3. Resolve a handle (e.g. @SomeChannel, a bare username, or a
        //    youtube.com/@SomeChannel URL) via channels.list?forHandle=
        try {
            val handleFromUrl = Regex("youtube\\.com/(@[\\w.-]+)").find(trimmed)?.groupValues?.get(1)
            val candidateHandle = when {
                handleFromUrl != null -> handleFromUrl
                trimmed.startsWith("@") -> trimmed
                trimmed.startsWith("http://") || trimmed.startsWith("https://") -> null
                else -> "@$trimmed"
            }

            if (candidateHandle != null) {
                val handleResponse = api.getChannelByHandle(handle = candidateHandle, apiKey = apiKey)
                handleResponse.items.firstOrNull()?.id?.let { return@withContext it }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Last resort: search YouTube for a channel matching the text
        try {
            val searchResponse = api.searchChannel(query = trimmed, apiKey = apiKey)
            searchResponse.items.firstOrNull()?.id?.channelId?.let { return@withContext it }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext null
    }

    companion object {
        // Safe channel ID parsing (kept for compatibility, unused internally now)
        fun extractChannelId(input: String): String? {
            val trimmed = input.trim()
            if (trimmed.startsWith("UC") && trimmed.length == 24) {
                return trimmed
            }
            val channelIdPattern = Regex("youtube\\.com/channel/(UC[a-zA-Z0-9_-]{22})")
            val match = channelIdPattern.find(trimmed)
            if (match != null) {
                return match.groupValues[1]
            }
            return null
        }
    }
}
