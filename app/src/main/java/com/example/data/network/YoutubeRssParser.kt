package com.example.data.network

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

/**
 * Parses the public `feeds/videos.xml` channel feed. Used as the fallback when
 * no YouTube Data API key is configured, or when the API call fails.
 */
object YoutubeRssParser {

    fun parseFeed(xmlString: String): List<YoutubeVideo> {
        val parsed = try {
            parseWithPullParser(xmlString)
        } catch (error: Exception) {
            emptyList()
        }
        return parsed.ifEmpty { parseWithRegex(xmlString) }
    }

    private fun parseWithPullParser(xmlString: String): List<YoutubeVideo> {
        val videos = mutableListOf<YoutubeVideo>()
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xmlString))

        var videoId = ""
        var title = ""
        var channelId = ""
        var channelName = ""
        var thumbnailUrl = ""
        var description = ""
        var published = ""
        var inEntry = false

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (name == "entry") {
                        inEntry = true
                        videoId = ""; title = ""; channelId = ""
                        channelName = ""; thumbnailUrl = ""; description = ""; published = ""
                    } else if (inEntry) {
                        when (name) {
                            "yt:videoId", "videoId" -> videoId = parser.nextText()
                            "yt:channelId", "channelId" -> channelId = parser.nextText()
                            "title" -> title = parser.nextText()
                            "published" -> published = parser.nextText()
                            "name" -> channelName = parser.nextText()
                            "media:thumbnail", "thumbnail" ->
                                parser.getAttributeValue(null, "url")?.takeIf { it.isNotBlank() }
                                    ?.let { thumbnailUrl = it }
                            "media:description", "description" -> description = parser.nextText()
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (name == "entry") {
                        inEntry = false
                        if (videoId.isNotEmpty()) {
                            videos.add(
                                buildVideo(
                                    videoId, title, channelId, channelName,
                                    thumbnailUrl, description, published,
                                )
                            )
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return videos
    }

    private fun parseWithRegex(xmlString: String): List<YoutubeVideo> {
        val videos = mutableListOf<YoutubeVideo>()
        try {
            for (match in ENTRY.findAll(xmlString)) {
                val entry = match.groupValues[1]
                val videoId = VIDEO_ID.find(entry)?.groupValues?.get(1).orEmpty()
                if (videoId.isEmpty()) continue
                videos.add(
                    buildVideo(
                        videoId = videoId,
                        title = decodeHtmlEntities(TITLE.find(entry)?.groupValues?.get(1).orEmpty()),
                        channelId = CHANNEL_ID.find(entry)?.groupValues?.get(1).orEmpty(),
                        channelName = decodeHtmlEntities(CHANNEL_NAME.find(entry)?.groupValues?.get(1).orEmpty()),
                        thumbnailUrl = THUMBNAIL.find(entry)?.groupValues?.get(1).orEmpty(),
                        description = decodeHtmlEntities(DESCRIPTION.find(entry)?.groupValues?.get(1).orEmpty()),
                        published = PUBLISHED.find(entry)?.groupValues?.get(1).orEmpty(),
                    )
                )
            }
        } catch (error: Exception) {
            error.printStackTrace()
        }
        return videos
    }

    private fun buildVideo(
        videoId: String,
        title: String,
        channelId: String,
        channelName: String,
        thumbnailUrl: String,
        description: String,
        published: String,
    ): YoutubeVideo {
        val publishedMillis = YoutubeTimeFormat.parseIsoToMillis(published)
        return YoutubeVideo(
            id = videoId,
            title = title,
            channelId = channelId,
            channelName = channelName,
            thumbnailUrl = thumbnailUrl.ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" },
            description = description,
            publishedAt = YoutubeTimeFormat.relativeHebrew(publishedMillis),
            publishedAtMillis = publishedMillis,
        )
    }

    private fun decodeHtmlEntities(text: String): String = text
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&#39;", "'")
        .replace("&amp;", "&")

    private val ENTRY = Regex("<entry>([\\s\\S]*?)</entry>")
    private val VIDEO_ID = Regex("<(?:yt:)?videoId>([^<]+)</(?:yt:)?videoId>")
    private val TITLE = Regex("<title>([^<]*)</title>")
    private val CHANNEL_ID = Regex("<(?:yt:)?channelId>([^<]+)</(?:yt:)?channelId>")
    private val CHANNEL_NAME = Regex("<name>([^<]*)</name>")
    private val THUMBNAIL = Regex("<(?:media:)?thumbnail[^>]+url=\"([^\"]+)\"")
    private val DESCRIPTION = Regex("<(?:media:)?description>([^<]*)</(?:media:)?description>")
    private val PUBLISHED = Regex("<published>([^<]+)</published>")
}
