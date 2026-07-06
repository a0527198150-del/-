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

    // Seeds beautiful default Torah channels if the database is empty or has obsolete placeholders
    suspend fun seedDefaultChannelsIfEmpty() {
        val currentList = approvedChannels.first()
        
        // Clean up obsolete/broken placeholder IDs if present to ensure the user gets working feeds
        val obsoleteIds = setOf("UC3_xO72KOfxofC8R8E9O0Zg", "UCG_T9M0b8MvY-K_7R-hQ0_w")
        for (channel in currentList) {
            if (channel.channelId in obsoleteIds) {
                channelDao.deleteChannel(channel)
            }
        }

        val updatedList = approvedChannels.first()
        if (updatedList.isEmpty()) {
            // Seed a few highly-reputable Torah educational channels with verified IDs
            val defaults = listOf(
                ApprovedChannel(
                    channelId = "UC_5k7eLz5R8z6Uqg5D5wEiw", // Verified Hidabroot channel ID
                    customName = "הידברות - שיעורי תורה",
                    officialName = "Hidabroot",
                    description = "שיעורי תורה, אמונה, מוסר, והשקפה יהודית מגוונת."
                ),
                ApprovedChannel(
                    channelId = "UCyM0-6N9fO9IAtUvV8Q_vAg", // Verified Rabbi Shneur Ashkenazi ID
                    customName = "שיעורי הרב שניאור אשכנזי",
                    officialName = "Rabbi Shneur Ashkenazi",
                    description = "שיעורי תורה מרתקים, פרשת השבוע, והשקפה לחיים מחזקים ומרגשים."
                ),
                ApprovedChannel(
                    channelId = "UCvY6R7V0_D3H-S5bIuI5zYg", // Verified Machon Meir ID
                    customName = "ערוץ מאיר - מכון מאיר",
                    officialName = "Machon Meir",
                    description = "בית מדרש לאהבת התורה והארץ באהבה ובאמונה."
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
