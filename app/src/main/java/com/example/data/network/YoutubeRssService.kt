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
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept-Language", "he-IL,he;q=0.9,en-US;q=0.8,en;q=0.7")
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

    suspend fun resolveChannelId(input: String): String? = withContext(Dispatchers.IO) {
        val trimmed = input.trim()

        // 1. If it's already a UC ID
        if (trimmed.startsWith("UC") && trimmed.length == 24) {
            return@withContext trimmed
        }

        // 2. If it's a channel URL with UC ID in it
        val channelIdPattern = Regex("youtube\\.com/channel/(UC[a-zA-Z0-9_-]{22})")
        val match = channelIdPattern.find(trimmed)
        if (match != null) {
            return@withContext match.groupValues[1]
        }

        // 3. Reconstruct the full YouTube URL
        val targetUrl = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("@") -> "https://www.youtube.com/$trimmed"
            else -> "https://www.youtube.com/@$trimmed"
        }

        val request = Request.Builder()
            .url(targetUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept-Language", "he-IL,he;q=0.9,en-US;q=0.8,en;q=0.7")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val html = response.body?.string() ?: ""

                // A. itemprop="channelId" content="UC..."
                val itempropRegex = Regex("itemprop=\"channelId\"\\s+content=\"(UC[a-zA-Z0-9_-]{22})\"")
                itempropRegex.find(html)?.groupValues?.get(1)?.let { return@withContext it }

                val itempropRegex2 = Regex("content=\"(UC[a-zA-Z0-9_-]{22})\"\\s+itemprop=\"channelId\"")
                itempropRegex2.find(html)?.groupValues?.get(1)?.let { return@withContext it }

                // B. canonical link: href="https://www.youtube.com/channel/UC..."
                val canonicalRegex = Regex("<link\\s+rel=\"canonical\"\\s+href=\"https://www.youtube.com/channel/(UC[a-zA-Z0-9_-]{22})\"")
                canonicalRegex.find(html)?.groupValues?.get(1)?.let { return@withContext it }

                // C. browseId or channelId in JSON structures
                val browseIdRegex = Regex("\"browseId\"\\s*:\\s*\"(UC[a-zA-Z0-9_-]{22})\"")
                browseIdRegex.find(html)?.groupValues?.get(1)?.let { return@withContext it }

                val jsonChannelIdRegex = Regex("\"channelId\"\\s*:\\s*\"(UC[a-zA-Z0-9_-]{22})\"")
                jsonChannelIdRegex.find(html)?.groupValues?.get(1)?.let { return@withContext it }

                val externalIdRegex = Regex("\"externalId\"\\s*:\\s*\"(UC[a-zA-Z0-9_-]{22})\"")
                externalIdRegex.find(html)?.groupValues?.get(1)?.let { return@withContext it }

                // D. General fallback for any channel link inside the page
                val generalChannelUrlRegex = Regex("/channel/(UC[a-zA-Z0-9_-]{22})")
                generalChannelUrlRegex.find(html)?.groupValues?.get(1)?.let { return@withContext it }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Fallback: Search YouTube with "channel" filters if it's a plain search query
        try {
            val encodedQuery = java.net.URLEncoder.encode(trimmed, "UTF-8")
            // sp=EgIQAg%253D%253D triggers the "Channel" filter on YouTube search
            val searchUrl = "https://www.youtube.com/results?search_query=$encodedQuery&sp=EgIQAg%253D%253D"
            val searchRequest = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept-Language", "he-IL,he;q=0.9,en-US;q=0.8,en;q=0.7")
                .build()

            client.newCall(searchRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string() ?: ""
                    
                    val browseIdRegex = Regex("\"browseId\"\\s*:\\s*\"(UC[a-zA-Z0-9_-]{22})\"")
                    browseIdRegex.find(html)?.groupValues?.get(1)?.let { return@withContext it }

                    val channelIdRegex = Regex("\"channelId\"\\s*:\\s*\"(UC[a-zA-Z0-9_-]{22})\"")
                    channelIdRegex.find(html)?.groupValues?.get(1)?.let { return@withContext it }
                    
                    val generalChannelUrlRegex = Regex("/channel/(UC[a-zA-Z0-9_-]{22})")
                    generalChannelUrlRegex.find(html)?.groupValues?.get(1)?.let { return@withContext it }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext null
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
