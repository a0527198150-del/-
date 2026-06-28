package com.example.data.network

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

object YoutubeRssParser {
    fun parseFeed(xmlString: String): List<YoutubeVideo> {
        val videos = mutableListOf<YoutubeVideo>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(xmlString))
            var eventType = parser.eventType
            
            var currentVideoId = ""
            var currentTitle = ""
            var currentChannelId = ""
            var currentChannelName = ""
            var currentThumbnailUrl = ""
            var currentDescription = ""
            var currentPublished = ""
            
            var inEntry = false
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name == "entry") {
                            inEntry = true
                            currentVideoId = ""
                            currentTitle = ""
                            currentChannelId = ""
                            currentChannelName = ""
                            currentThumbnailUrl = ""
                            currentDescription = ""
                            currentPublished = ""
                        } else if (inEntry) {
                            when (name) {
                                "yt:videoId" -> currentVideoId = parser.nextText()
                                "yt:channelId" -> currentChannelId = parser.nextText()
                                "title" -> currentTitle = parser.nextText()
                                "published" -> currentPublished = parser.nextText()
                                "name" -> currentChannelName = parser.nextText()
                                "media:thumbnail" -> {
                                    val url = parser.getAttributeValue(null, "url")
                                    if (!url.isNullOrBlank()) {
                                        currentThumbnailUrl = url
                                    }
                                }
                                "media:description" -> currentDescription = parser.nextText()
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name == "entry") {
                            inEntry = false
                            if (currentVideoId.isNotEmpty()) {
                                if (currentThumbnailUrl.isEmpty()) {
                                    currentThumbnailUrl = "https://i.ytimg.com/vi/$currentVideoId/hqdefault.jpg"
                                }
                                videos.add(
                                    YoutubeVideo(
                                        id = currentVideoId,
                                        title = currentTitle,
                                        channelId = currentChannelId,
                                        channelName = currentChannelName,
                                        thumbnailUrl = currentThumbnailUrl,
                                        description = currentDescription,
                                        publishedAt = currentPublished
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: Use regex parser on failure to ensure reliable parsing
            return parseFeedWithRegex(xmlString)
        }
        return if (videos.isEmpty()) parseFeedWithRegex(xmlString) else videos
    }

    private fun parseFeedWithRegex(xmlString: String): List<YoutubeVideo> {
        val videos = mutableListOf<YoutubeVideo>()
        try {
            // Segment xml entry strings
            val entryRegex = Regex("<entry>([\\s\\S]*?)</entry>")
            val videoIdRegex = Regex("<yt:videoId>([^<]+)</yt:videoId>")
            val titleRegex = Regex("<title>([^<]+)</title>")
            val channelIdRegex = Regex("<yt:channelId>([^<]+)</yt:channelId>")
            val channelNameRegex = Regex("<name>([^<]+)</name>")
            val thumbnailRegex = Regex("<media:thumbnail[^>]+url=\"([^\"]+)\"")
            val descriptionRegex = Regex("<media:description>([^<]*)</media:description>")
            val publishedRegex = Regex("<published>([^<]+)</published>")

            entryRegex.findAll(xmlString).forEach { matchResult ->
                val entryContent = matchResult.groupValues[1]
                val videoId = videoIdRegex.find(entryContent)?.groupValues?.get(1) ?: ""
                val title = titleRegex.find(entryContent)?.groupValues?.get(1) ?: ""
                val channelId = channelIdRegex.find(entryContent)?.groupValues?.get(1) ?: ""
                val channelName = channelNameRegex.find(entryContent)?.groupValues?.get(1) ?: ""
                val thumbnailUrl = thumbnailRegex.find(entryContent)?.groupValues?.get(1) ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                val description = descriptionRegex.find(entryContent)?.groupValues?.get(1) ?: ""
                val publishedAt = publishedRegex.find(entryContent)?.groupValues?.get(1) ?: ""

                if (videoId.isNotEmpty()) {
                    videos.add(
                        YoutubeVideo(
                            id = videoId,
                            title = decodeHtmlEntities(title),
                            channelId = channelId,
                            channelName = decodeHtmlEntities(channelName),
                            thumbnailUrl = thumbnailUrl,
                            description = decodeHtmlEntities(description),
                            publishedAt = publishedAt
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return videos
    }

    private fun decodeHtmlEntities(text: String): String {
        return text
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
    }
}
