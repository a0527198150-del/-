package com.example.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** Outcome of checking a user supplied YouTube Data API key. */
sealed interface ApiKeyCheck {
    object Valid : ApiKeyCheck
    data class Invalid(val reason: String) : ApiKeyCheck
}

/**
 * All YouTube access in one place.
 *
 * Every entry point takes the API key as a parameter: the key is supplied by the
 * user at runtime and stored in [com.example.data.settings.SettingsRepository],
 * never compiled into the APK. When no key is configured the source silently
 * degrades to the public RSS feed and, failing that, to page scraping.
 */
class YoutubeDataSource {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // region public API

    suspend fun fetchChannelVideos(
        channelId: String,
        apiKey: String,
        maxResults: Int = 30,
    ): List<YoutubeVideo> = withContext(Dispatchers.IO) {
        if (apiKey.isNotBlank()) {
            val viaApi = fetchVideosViaApi(channelId, apiKey, maxResults)
            if (viaApi.isNotEmpty()) return@withContext viaApi
        }

        val viaRss = fetchVideosViaRss(channelId)
        if (viaRss.isNotEmpty()) return@withContext viaRss

        for (url in listOf(
            "https://www.youtube.com/channel/$channelId/videos",
            "https://www.youtube.com/channel/$channelId",
        )) {
            val scraped = get(url)?.let { extractVideosFromHtml(it, channelId) }.orEmpty()
            if (scraped.isNotEmpty()) return@withContext scraped
        }

        emptyList()
    }

    suspend fun fetchChannelInfo(
        channelId: String,
        apiKey: String,
    ): YoutubeChannelInfo? = withContext(Dispatchers.IO) {
        if (apiKey.isNotBlank()) {
            val body = get("$API_BASE/channels?part=snippet&id=$channelId&key=$apiKey")
            val parsed = body?.let { parseChannelInfo(it) }
            if (parsed != null) return@withContext parsed
        }
        val html = get("https://www.youtube.com/channel/$channelId") ?: return@withContext null
        val title = metaContent(html, "og:title")?.removeSuffix(" - YouTube") ?: return@withContext null
        YoutubeChannelInfo(
            channelId = channelId,
            title = title,
            description = metaContent(html, "og:description").orEmpty(),
            avatarUrl = metaContent(html, "og:image").orEmpty(),
        )
    }

    /**
     * Turns a channel id, channel URL, `@handle` or plain search term into a
     * canonical `UC…` channel id.
     */
    suspend fun resolveChannelId(input: String, apiKey: String): String? = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return@withContext null

        canonicalChannelId(trimmed)?.let { return@withContext it }

        if (apiKey.isNotBlank()) {
            resolveViaApi(trimmed, apiKey)?.let { return@withContext it }
        }

        val targetUrl = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("@") -> "https://www.youtube.com/$trimmed"
            else -> "https://www.youtube.com/@$trimmed"
        }
        get(targetUrl)?.let { html -> channelIdFromHtml(html)?.let { return@withContext it } }

        val query = URLEncoder.encode(trimmed, "UTF-8")
        // sp=EgIQAg%253D%253D restricts YouTube search to channels only.
        val searchUrl = "https://www.youtube.com/results?search_query=$query&sp=EgIQAg%253D%253D"
        get(searchUrl)?.let { html -> channelIdFromHtml(html)?.let { return@withContext it } }

        null
    }

    /** Performs a single cheap request so the user gets immediate feedback on their key. */
    suspend fun checkApiKey(apiKey: String): ApiKeyCheck = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        if (key.isBlank()) return@withContext ApiKeyCheck.Invalid("לא הוזן מפתח.")
        val url = "$API_BASE/channels?part=id&id=$VERIFICATION_CHANNEL_ID&key=$key"
        try {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) return@withContext ApiKeyCheck.Valid
                val reason = JSONObject(body)
                    .optJSONObject("error")
                    ?.optString("message")
                    .orEmpty()
                ApiKeyCheck.Invalid(
                    when {
                        reason.contains("API key not valid", true) -> "המפתח אינו תקין. ודא שהעתקת אותו במלואו."
                        reason.contains("has not been used", true) ||
                            reason.contains("disabled", true) ->
                            "יש להפעיל את YouTube Data API v3 בפרויקט ב-Google Cloud."
                        reason.contains("quota", true) -> "חרגת ממכסת השימוש היומית של המפתח."
                        reason.isNotBlank() -> reason
                        else -> "המפתח נדחה על ידי Google (קוד ${response.code})."
                    }
                )
            }
        } catch (error: Exception) {
            ApiKeyCheck.Invalid("אין חיבור לאינטרנט או שהשרת אינו זמין כרגע.")
        }
    }

    // endregion

    // region YouTube Data API v3

    private fun fetchVideosViaApi(channelId: String, apiKey: String, maxResults: Int): List<YoutubeVideo> {
        val capped = maxResults.coerceIn(1, 50)

        // The uploads playlist of a channel is its id with the UC prefix swapped for UU.
        if (channelId.startsWith("UC") && channelId.length == 24) {
            val uploadsPlaylistId = "UU" + channelId.substring(2)
            val body = get(
                "$API_BASE/playlistItems?part=snippet&maxResults=$capped" +
                    "&playlistId=$uploadsPlaylistId&key=$apiKey"
            )
            val videos = body?.let { parsePlaylistItems(it, channelId) }.orEmpty()
            if (videos.isNotEmpty()) return withDurations(videos, apiKey)
        }

        val body = get(
            "$API_BASE/search?part=snippet&maxResults=$capped&channelId=$channelId" +
                "&order=date&type=video&key=$apiKey"
        )
        val videos = body?.let { parseSearchItems(it, channelId) }.orEmpty()
        return if (videos.isEmpty()) videos else withDurations(videos, apiKey)
    }

    /** One extra call fills in the runtime badge shown on every card. */
    private fun withDurations(videos: List<YoutubeVideo>, apiKey: String): List<YoutubeVideo> {
        val ids = videos.map { it.id }.distinct().take(50)
        if (ids.isEmpty()) return videos
        val body = get("$API_BASE/videos?part=contentDetails&id=${ids.joinToString(",")}&key=$apiKey")
            ?: return videos
        val durations = mutableMapOf<String, Int>()
        try {
            val items = JSONObject(body).optJSONArray("items") ?: return videos
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val id = item.optString("id")
                val iso = item.optJSONObject("contentDetails")?.optString("duration").orEmpty()
                if (id.isNotBlank()) durations[id] = YoutubeTimeFormat.parseIsoDuration(iso)
            }
        } catch (error: Exception) {
            return videos
        }
        return videos.map { video -> video.copy(durationSeconds = durations[video.id] ?: 0) }
    }

    private fun parsePlaylistItems(json: String, channelId: String): List<YoutubeVideo> = parseItems(json) { item ->
        val snippet = item.optJSONObject("snippet") ?: return@parseItems null
        val videoId = snippet.optJSONObject("resourceId")?.optString("videoId").orEmpty()
        if (videoId.isBlank()) return@parseItems null
        videoFromSnippet(videoId, snippet, channelId)
    }

    private fun parseSearchItems(json: String, channelId: String): List<YoutubeVideo> = parseItems(json) { item ->
        val videoId = item.optJSONObject("id")?.optString("videoId").orEmpty()
        val snippet = item.optJSONObject("snippet")
        if (videoId.isBlank() || snippet == null) return@parseItems null
        videoFromSnippet(videoId, snippet, channelId)
    }

    private fun parseItems(json: String, mapper: (JSONObject) -> YoutubeVideo?): List<YoutubeVideo> {
        return try {
            val items = JSONObject(json).optJSONArray("items") ?: return emptyList()
            val videos = mutableListOf<YoutubeVideo>()
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                mapper(item)?.let { videos.add(it) }
            }
            videos
        } catch (error: Exception) {
            emptyList()
        }
    }

    private fun videoFromSnippet(videoId: String, snippet: JSONObject, channelId: String): YoutubeVideo {
        val publishedRaw = snippet.optString("publishedAt")
        val publishedMillis = YoutubeTimeFormat.parseIsoToMillis(publishedRaw)
        return YoutubeVideo(
            id = videoId,
            title = snippet.optString("title", "שיעור"),
            channelId = channelId,
            channelName = snippet.optString("channelTitle", ""),
            thumbnailUrl = bestThumbnail(snippet.optJSONObject("thumbnails"), videoId),
            description = snippet.optString("description", ""),
            publishedAt = YoutubeTimeFormat.relativeHebrew(publishedMillis),
            publishedAtMillis = publishedMillis,
        )
    }

    private fun bestThumbnail(thumbnails: JSONObject?, videoId: String): String {
        val node = thumbnails?.optJSONObject("maxres")
            ?: thumbnails?.optJSONObject("standard")
            ?: thumbnails?.optJSONObject("high")
            ?: thumbnails?.optJSONObject("medium")
            ?: thumbnails?.optJSONObject("default")
        val url = node?.optString("url").orEmpty()
        return url.ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" }
    }

    private fun parseChannelInfo(json: String): YoutubeChannelInfo? {
        return try {
            val items = JSONObject(json).optJSONArray("items") ?: return null
            val item = items.optJSONObject(0) ?: return null
            val id = item.optString("id")
            val snippet = item.optJSONObject("snippet") ?: return null
            val thumbnails = snippet.optJSONObject("thumbnails")
            val avatar = (
                thumbnails?.optJSONObject("high")
                    ?: thumbnails?.optJSONObject("medium")
                    ?: thumbnails?.optJSONObject("default")
                )?.optString("url").orEmpty()
            YoutubeChannelInfo(
                channelId = id,
                title = snippet.optString("title"),
                description = snippet.optString("description"),
                avatarUrl = avatar,
            )
        } catch (error: Exception) {
            null
        }
    }

    private fun resolveViaApi(input: String, apiKey: String): String? {
        val handle = when {
            input.contains("youtube.com/@") ->
                "@" + input.substringAfter("youtube.com/@").substringBefore("/").substringBefore("?")
            input.startsWith("@") -> input
            else -> null
        }

        if (handle != null) {
            val body = get("$API_BASE/channels?part=id&forHandle=$handle&key=$apiKey")
            parseChannelInfo(body.orEmpty())?.channelId?.takeIf { it.isNotBlank() }?.let { return it }
        }

        if (!input.startsWith("http")) {
            val query = URLEncoder.encode(input, "UTF-8")
            val body = get("$API_BASE/search?part=snippet&type=channel&maxResults=1&q=$query&key=$apiKey")
            try {
                val items = JSONObject(body.orEmpty()).optJSONArray("items")
                val id = items?.optJSONObject(0)?.optJSONObject("id")?.optString("channelId")
                if (!id.isNullOrBlank()) return id
            } catch (error: Exception) {
                // fall through to the scraping path
            }
        }
        return null
    }

    // endregion

    // region RSS + scraping fallbacks

    private fun fetchVideosViaRss(channelId: String): List<YoutubeVideo> {
        val body = get("https://www.youtube.com/feeds/videos.xml?channel_id=$channelId") ?: return emptyList()
        return YoutubeRssParser.parseFeed(body)
    }

    private fun extractVideosFromHtml(html: String, channelId: String): List<YoutubeVideo> {
        val videos = mutableListOf<YoutubeVideo>()
        try {
            val channelName = metaContent(html, "og:title")?.removeSuffix(" - YouTube").orEmpty()
            for (match in VIDEO_RENDERER.findAll(html)) {
                val content = match.groupValues[1]
                val videoId = VIDEO_ID.find(content)?.groupValues?.get(1) ?: continue
                if (videos.any { it.id == videoId }) continue

                val title = decodeUnicode(
                    TITLE_RUNS.find(content)?.groupValues?.get(1)
                        ?: TITLE_SIMPLE.find(content)?.groupValues?.get(1)
                        ?: LABEL.find(content)?.groupValues?.get(1)
                        ?: "שיעור"
                )
                val thumbnail = THUMBNAIL.find(content)?.groupValues?.get(1)?.replace("\\/", "/")
                    ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                val published = PUBLISHED_TEXT.find(content)?.groupValues?.get(1)?.let { decodeUnicode(it) }.orEmpty()
                val description = DESCRIPTION_SNIPPET.find(content)?.groupValues?.get(1)
                    ?.let { decodeUnicode(it) }.orEmpty()
                val lengthText = LENGTH_TEXT.find(content)?.groupValues?.get(1).orEmpty()

                videos.add(
                    YoutubeVideo(
                        id = videoId,
                        title = title,
                        channelId = channelId,
                        channelName = channelName,
                        thumbnailUrl = thumbnail,
                        description = description,
                        publishedAt = published,
                        durationSeconds = parseClockText(lengthText),
                    )
                )
            }
        } catch (error: Exception) {
            error.printStackTrace()
        }
        return videos
    }

    /** `1:02:10` / `12:30` as shown on youtube.com, back into seconds. */
    private fun parseClockText(text: String): Int {
        val parts = text.trim().split(":").mapNotNull { it.trim().toIntOrNull() }
        return when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> 0
        }
    }

    private fun canonicalChannelId(input: String): String? {
        if (input.startsWith("UC") && input.length == 24) return input
        return Regex("youtube\\.com/channel/(UC[a-zA-Z0-9_-]{22})").find(input)?.groupValues?.get(1)
    }

    private fun channelIdFromHtml(html: String): String? {
        for (pattern in CHANNEL_ID_PATTERNS) {
            pattern.find(html)?.groupValues?.get(1)?.let { return it }
        }
        return null
    }

    private fun metaContent(html: String, property: String): String? =
        Regex("<meta[^>]+property=\"$property\"[^>]+content=\"([^\"]*)\"").find(html)?.groupValues?.get(1)
            ?: Regex("<meta[^>]+content=\"([^\"]*)\"[^>]+property=\"$property\"").find(html)?.groupValues?.get(1)

    private fun decodeUnicode(input: String): String {
        if (!input.contains("\\u")) return input
        return try {
            Regex("\\\\u([0-9a-fA-F]{4})").replace(input) { match ->
                match.groupValues[1].toInt(16).toChar().toString()
            }
        } catch (error: Exception) {
            input
        }
    }

    /** Single place where every HTTP GET in this class goes through. */
    private fun get(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", DESKTOP_USER_AGENT)
                .header("Accept-Language", "he-IL,he;q=0.9,en-US;q=0.8,en;q=0.7")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (error: Exception) {
            null
        }
    }

    // endregion

    private companion object {
        const val API_BASE = "https://www.googleapis.com/youtube/v3"

        /** YouTube's own channel: a stable id to validate a key against. */
        const val VERIFICATION_CHANNEL_ID = "UCBR8-60-B28hp2BmDPdntcQ"

        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        val VIDEO_RENDERER = Regex("\"videoRenderer\"\\s*:\\s*\\{([\\s\\S]*?)\\}")
        val VIDEO_ID = Regex("\"videoId\"\\s*:\\s*\"([a-zA-Z0-9_-]{11})\"")
        val TITLE_RUNS = Regex("\"title\"\\s*:\\s*\\{\\s*\"runs\"\\s*:\\s*\\[\\s*\\{\\s*\"text\"\\s*:\\s*\"([^\"]+)\"")
        val TITLE_SIMPLE = Regex("\"title\"\\s*:\\s*\\{\\s*\"simpleText\"\\s*:\\s*\"([^\"]+)\"")
        val LABEL = Regex("\"label\"\\s*:\\s*\"([^\"]+)\"")
        val THUMBNAIL = Regex("\"url\"\\s*:\\s*\"(https://i\\.ytimg\\.com/[^\"]+)\"")
        val PUBLISHED_TEXT = Regex("\"publishedTimeText\"\\s*:\\s*\\{\\s*\"simpleText\"\\s*:\\s*\"([^\"]+)\"")
        val DESCRIPTION_SNIPPET =
            Regex("\"descriptionSnippet\"\\s*:\\s*\\{\\s*\"runs\"\\s*:\\s*\\[\\s*\\{\\s*\"text\"\\s*:\\s*\"([^\"]+)\"")
        val LENGTH_TEXT = Regex("\"lengthText\"\\s*:\\s*\\{[^}]*\"simpleText\"\\s*:\\s*\"([0-9:]+)\"")

        val CHANNEL_ID_PATTERNS = listOf(
            Regex("itemprop=\"channelId\"\\s+content=\"(UC[a-zA-Z0-9_-]{22})\""),
            Regex("content=\"(UC[a-zA-Z0-9_-]{22})\"\\s+itemprop=\"channelId\""),
            Regex("<link\\s+rel=\"canonical\"\\s+href=\"https://www\\.youtube\\.com/channel/(UC[a-zA-Z0-9_-]{22})\""),
            Regex("\"browseId\"\\s*:\\s*\"(UC[a-zA-Z0-9_-]{22})\""),
            Regex("\"externalId\"\\s*:\\s*\"(UC[a-zA-Z0-9_-]{22})\""),
            Regex("\"channelId\"\\s*:\\s*\"(UC[a-zA-Z0-9_-]{22})\""),
            Regex("/channel/(UC[a-zA-Z0-9_-]{22})"),
        )
    }
}
