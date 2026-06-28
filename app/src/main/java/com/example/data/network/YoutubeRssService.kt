package com.example.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class YoutubeRssService {
    private val client = OkHttpClient()

    suspend fun fetchChannelVideos(channelId: String): List<YoutubeVideo> = withContext(Dispatchers.IO) {
        val url = "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected HTTP code $response")
                val bodyString = response.body?.string() ?: ""
                return@withContext YoutubeRssParser.parseFeed(bodyString)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    companion object {
        // Safe channel ID parsing
        fun extractChannelId(input: String): String? {
            val trimmed = input.trim()
            if (trimmed.startsWith("UC") && trimmed.length == 24) {
                return trimmed
            }
            
            // Matches youtube.com/channel/UCxxxxxxxxxxxxxxxxx
            val channelIdPattern = Regex("youtube\\.com/channel/(UC[a-zA-Z0-9_-]{22})")
            val match = channelIdPattern.find(trimmed)
            if (match != null) {
                return match.groupValues[1]
            }

            // Matches youtube.com/c/xxx or youtube.com/@username
            // Note: RSS feed only supports UC... format, so we can display instructions 
            // on how to find the UC... channel ID if they put a username.
            return null
        }
    }
}
