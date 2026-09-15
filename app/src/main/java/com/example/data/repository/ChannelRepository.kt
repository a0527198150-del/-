package com.example.data.repository

import com.example.data.database.ApprovedChannel
import com.example.data.database.ChannelDao
import com.example.data.database.SupervisorSettings
import com.example.data.network.YoutubeChannelInfo
import com.example.data.network.YoutubeDataSource
import com.example.data.network.YoutubeVideo
import com.example.data.settings.SettingsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Result of trying to approve a new channel, so the UI can say exactly what happened. */
sealed interface AddChannelResult {
    data class Added(val channel: ApprovedChannel) : AddChannelResult
    data class Failed(val message: String) : AddChannelResult
}

class ChannelRepository(
    private val channelDao: ChannelDao,
    private val settings: SettingsRepository,
    private val youtube: YoutubeDataSource = YoutubeDataSource(),
) {
    val approvedChannels: Flow<List<ApprovedChannel>> = channelDao.getAllApprovedChannels()

    suspend fun fetchChannelVideos(channelId: String, maxResults: Int = 30): List<YoutubeVideo> =
        youtube.fetchChannelVideos(channelId, settings.currentApiKey(), maxResults)

    /** Latest uploads across every approved channel, newest first. */
    suspend fun fetchLatestAcrossChannels(perChannel: Int = 6): List<YoutubeVideo> = coroutineScope {
        val channels = approvedChannels.first()
        val apiKey = settings.currentApiKey()
        channels
            .map { channel ->
                async {
                    youtube.fetchChannelVideos(channel.channelId, apiKey, perChannel)
                        .take(perChannel)
                        .map { video ->
                            // Prefer the supervisor's own label over YouTube's channel title.
                            if (video.channelName.isBlank()) video.copy(channelName = channel.customName) else video
                        }
                }
            }
            .flatMap { it.await() }
            .sortedByDescending { it.publishedAtMillis }
    }

    suspend fun addChannel(rawInput: String, customName: String, description: String): AddChannelResult {
        val apiKey = settings.currentApiKey()
        val channelId = youtube.resolveChannelId(rawInput, apiKey)
            ?: return AddChannelResult.Failed(
                "לא הצלחנו לזהות ערוץ מהקלט. נסה קישור מלא לערוץ, שם משתמש עם @, או מזהה שמתחיל ב-UC."
            )

        if (channelDao.findChannel(channelId) != null) {
            return AddChannelResult.Failed("הערוץ כבר נמצא ברשימה המאושרת.")
        }

        val info = youtube.fetchChannelInfo(channelId, apiKey)
        val probeVideos = youtube.fetchChannelVideos(channelId, apiKey, maxResults = 3)
        if (info == null && probeVideos.isEmpty()) {
            return AddChannelResult.Failed("הערוץ זוהה אך לא נמצאו בו סרטונים זמינים. בדוק את החיבור לאינטרנט.")
        }

        val officialName = info?.title
            ?: probeVideos.firstOrNull()?.channelName?.takeIf { it.isNotBlank() }
            ?: customName.ifBlank { "ערוץ מאושר" }

        val channel = ApprovedChannel(
            channelId = channelId,
            customName = customName.ifBlank { officialName },
            officialName = officialName,
            avatarUrl = info?.avatarUrl.orEmpty(),
            description = description.ifBlank {
                info?.description?.take(160).orEmpty().ifBlank { "ערוץ שאושר לצפייה מסוננת." }
            },
        )
        channelDao.insertChannel(channel)
        return AddChannelResult.Added(channel)
    }

    suspend fun removeChannel(channel: ApprovedChannel) = channelDao.deleteChannel(channel)

    /** Re-pulls avatars and official names, e.g. after an API key is added. */
    suspend fun refreshChannelMetadata(onlyMissing: Boolean = true): Int {
        val apiKey = settings.currentApiKey()
        var updated = 0
        for (channel in approvedChannels.first()) {
            if (onlyMissing && channel.avatarUrl.isNotBlank()) continue
            val info: YoutubeChannelInfo = youtube.fetchChannelInfo(channel.channelId, apiKey) ?: continue
            channelDao.insertChannel(
                channel.copy(
                    officialName = info.title.ifBlank { channel.officialName },
                    avatarUrl = info.avatarUrl.ifBlank { channel.avatarUrl },
                    description = channel.description.ifBlank { info.description.take(160) },
                )
            )
            updated++
        }
        return updated
    }

    suspend fun getSupervisorPin(): String {
        val stored = channelDao.getSupervisorSettings()
        if (stored != null) return stored.supervisorPin
        val defaults = SupervisorSettings()
        channelDao.insertSupervisorSettings(defaults)
        return defaults.supervisorPin
    }

    suspend fun updateSupervisorPin(newPin: String) {
        val current = channelDao.getSupervisorSettings() ?: SupervisorSettings()
        channelDao.insertSupervisorSettings(current.copy(supervisorPin = newPin))
    }

    /** Populates a first-run library so the app is never empty on install. */
    suspend fun seedDefaultChannelsIfEmpty() {
        for (channel in approvedChannels.first()) {
            if (channel.channelId in OBSOLETE_SEED_IDS) channelDao.deleteChannel(channel)
        }

        if (approvedChannels.first().isEmpty()) {
            DEFAULT_CHANNELS.forEach { channelDao.insertChannel(it) }
        }

        if (channelDao.getSupervisorSettings() == null) {
            channelDao.insertSupervisorSettings(SupervisorSettings())
        }
    }

    private companion object {
        /** Placeholder ids shipped by earlier builds that never resolved to a real feed. */
        val OBSOLETE_SEED_IDS = setOf(
            "UC3_xO72KOfxofC8R8E9O0Zg",
            "UCG_T9M0b8MvY-K_7R-hQ0_w",
            "UC_5k7eLz5R8z6Uqg5D5wEiw",
            "UCyM0-6N9fO9IAtUvV8Q_vAg",
            "UCvY6R7V0_D3H-S5bIuI5zYg",
        )

        val DEFAULT_CHANNELS = listOf(
            ApprovedChannel(
                channelId = "UCzqiIOm_TUH0lG3pwlbMkyA",
                customName = "הידברות",
                officialName = "הידברות",
                description = "שיעורי תורה, אמונה, מוסר והשקפה יהודית.",
            ),
            ApprovedChannel(
                channelId = "UC1BIlJW-jVw_EUnj0p-LIgQ",
                customName = "הרב שניאור אשכנזי",
                officialName = "הרב שניאור אשכנזי",
                description = "שיעורי תורה, פרשת השבוע והשקפה לחיים.",
            ),
            ApprovedChannel(
                channelId = "UCEAZVyOtukIOH4BJ3gHKdng",
                customName = "ערוץ מאיר",
                officialName = "ערוץ מאיר - מכון מאיר",
                description = "בית מדרש לאהבת התורה והארץ, באהבה ובאמונה.",
            ),
        )
    }
}
