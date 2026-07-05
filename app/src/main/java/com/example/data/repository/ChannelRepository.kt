package com.example.data.repository

import com.example.data.database.ApprovedChannel
import com.example.data.database.ChannelDao
import com.example.data.database.SupervisorSettings
import com.example.data.network.YoutubeRssService
import com.example.data.network.YoutubeVideo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ChannelRepository(
    private val channelDao: ChannelDao,
    private val rssService: YoutubeRssService = YoutubeRssService()
) {
    val approvedChannels: Flow<List<ApprovedChannel>> = channelDao.getAllApprovedChannels()

    suspend fun addChannel(channelId: String, customName: String, officialName: String, description: String = "") {
        val channel = ApprovedChannel(
            channelId = channelId,
            customName = customName.ifBlank { officialName },
            officialName = officialName,
            avatarUrl = "https://i.ytimg.com/vi/dummy/hqdefault.jpg", // placeholder
            description = description
        )
        channelDao.insertChannel(channel)
    }

    suspend fun removeChannel(channel: ApprovedChannel) {
        channelDao.deleteChannel(channel)
    }

    suspend fun getSupervisorPin(): String {
        val settings = channelDao.getSupervisorSettings()
        if (settings == null) {
            val defaultSettings = SupervisorSettings()
            channelDao.insertSupervisorSettings(defaultSettings)
            return defaultSettings.supervisorPin
        }
        return settings.supervisorPin
    }

    suspend fun updateSupervisorPin(newPin: String) {
        val currentSettings = channelDao.getSupervisorSettings() ?: SupervisorSettings()
        val updated = currentSettings.copy(supervisorPin = newPin)
        channelDao.insertSupervisorSettings(updated)
    }

    suspend fun fetchChannelVideos(channelId: String): List<YoutubeVideo> {
        return rssService.fetchChannelVideos(channelId)
    }

    suspend fun resolveChannelId(input: String): String? {
        return rssService.resolveChannelId(input)
    }

    // Seeds beautiful default Torah channels if the database is empty
    suspend fun seedDefaultChannelsIfEmpty() {
        val currentList = approvedChannels.first()
        if (currentList.isEmpty()) {
            // Seed a few highly-reputable Torah educational channels
            val defaults = listOf(
                ApprovedChannel(
                    channelId = "UC3_xO72KOfxofC8R8E9O0Zg", // Hidabroot Torah channel (placeholder / active)
                    customName = "הידברות - שיעורי תורה",
                    officialName = "Hidabroot",
                    description = "שיעורי תורה, אמונה, מוסר, והשקפה יהודית מגוונת."
                ),
                ApprovedChannel(
                    channelId = "UCvY6R7V0_D3H-S5bIuI5zYg", // Machon Meir (placeholder / active)
                    customName = "ערוץ מאיר - מכון מאיר",
                    officialName = "Machon Meir",
                    description = "בית מדרש לאהבת התורה והארץ באהבה ובאמונה."
                ),
                ApprovedChannel(
                    channelId = "UCG_T9M0b8MvY-K_7R-hQ0_w", // Torah lectures (placeholder / active)
                    customName = "שיעורי תורה קצרים ומעוררים",
                    officialName = "Torah Shorts",
                    description = "סרטוני חיזוק קצרים, הלכה יומית, ומוסר יומי מגדולי ישראל."
                )
            )
            for (channel in defaults) {
                channelDao.insertChannel(channel)
            }
        }

        // Initialize supervisor settings
        if (channelDao.getSupervisorSettings() == null) {
            channelDao.insertSupervisorSettings(SupervisorSettings())
        }
    }
}
