package com.example.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "kosher_settings")

/**
 * Everything the user configures at runtime. The YouTube Data API key lives here
 * rather than in BuildConfig, so it is asked for on first launch and can be
 * replaced from the supervisor panel without rebuilding the app.
 */
data class AppSettings(
    val youtubeApiKey: String = "",
    val setupCompleted: Boolean = false,
    val autoplay: Boolean = true,
    val playbackSpeed: Float = 1f,
) {
    val hasApiKey: Boolean get() = youtubeApiKey.isNotBlank()
}

/** A partially watched video, used to power the "continue watching" row. */
data class WatchProgress(
    val videoId: String,
    val title: String,
    val channelId: String,
    val channelName: String,
    val thumbnailUrl: String,
    val positionSeconds: Float,
    val durationSeconds: Float,
    val updatedAt: Long,
) {
    val fraction: Float
        get() = if (durationSeconds <= 0f) 0f else (positionSeconds / durationSeconds).coerceIn(0f, 1f)

    /** Anything past this point is treated as "finished" and drops off the row. */
    val isFinished: Boolean get() = fraction >= 0.97f
}

class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    private val preferences: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    val settings: Flow<AppSettings> = preferences.map { prefs ->
        AppSettings(
            youtubeApiKey = prefs[KEY_API_KEY].orEmpty(),
            setupCompleted = prefs[KEY_SETUP_DONE] ?: false,
            autoplay = prefs[KEY_AUTOPLAY] ?: true,
            playbackSpeed = prefs[KEY_SPEED] ?: 1f,
        )
    }

    val watchProgress: Flow<List<WatchProgress>> = preferences.map { prefs ->
        decodeProgress(prefs[KEY_PROGRESS].orEmpty())
    }

    suspend fun currentApiKey(): String = settings.first().youtubeApiKey

    suspend fun setApiKey(key: String) {
        dataStore.edit { it[KEY_API_KEY] = key.trim() }
    }

    suspend fun completeSetup() {
        dataStore.edit { it[KEY_SETUP_DONE] = true }
    }

    suspend fun setAutoplay(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTOPLAY] = enabled }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        dataStore.edit { it[KEY_SPEED] = speed }
    }

    suspend fun saveProgress(progress: WatchProgress) {
        // Too short to be meaningful, and finished videos should not linger.
        if (progress.durationSeconds < 30f) return
        dataStore.edit { prefs ->
            val merged = decodeProgress(prefs[KEY_PROGRESS].orEmpty())
                .filterNot { it.videoId == progress.videoId }
                .toMutableList()
            if (!progress.isFinished && progress.positionSeconds > 10f) {
                merged.add(progress)
            }
            val trimmed = merged.sortedByDescending { it.updatedAt }.take(MAX_PROGRESS_ENTRIES)
            prefs[KEY_PROGRESS] = encodeProgress(trimmed)
        }
    }

    suspend fun forgetProgress(videoId: String) {
        dataStore.edit { prefs ->
            val remaining = decodeProgress(prefs[KEY_PROGRESS].orEmpty()).filterNot { it.videoId == videoId }
            prefs[KEY_PROGRESS] = encodeProgress(remaining)
        }
    }

    suspend fun clearProgress() {
        dataStore.edit { it[KEY_PROGRESS] = "[]" }
    }

    private fun encodeProgress(items: List<WatchProgress>): String {
        val array = JSONArray()
        for (item in items) {
            array.put(
                JSONObject()
                    .put("videoId", item.videoId)
                    .put("title", item.title)
                    .put("channelId", item.channelId)
                    .put("channelName", item.channelName)
                    .put("thumbnailUrl", item.thumbnailUrl)
                    .put("position", item.positionSeconds.toDouble())
                    .put("duration", item.durationSeconds.toDouble())
                    .put("updatedAt", item.updatedAt)
            )
        }
        return array.toString()
    }

    private fun decodeProgress(raw: String): List<WatchProgress> {
        if (raw.isBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            val items = mutableListOf<WatchProgress>()
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val videoId = item.optString("videoId")
                if (videoId.isBlank()) continue
                items.add(
                    WatchProgress(
                        videoId = videoId,
                        title = item.optString("title"),
                        channelId = item.optString("channelId"),
                        channelName = item.optString("channelName"),
                        thumbnailUrl = item.optString("thumbnailUrl"),
                        positionSeconds = item.optDouble("position", 0.0).toFloat(),
                        durationSeconds = item.optDouble("duration", 0.0).toFloat(),
                        updatedAt = item.optLong("updatedAt", 0L),
                    )
                )
            }
            items.sortedByDescending { it.updatedAt }
        } catch (error: Exception) {
            emptyList()
        }
    }

    private companion object {
        const val MAX_PROGRESS_ENTRIES = 40

        val KEY_API_KEY = stringPreferencesKey("youtube_api_key")
        val KEY_SETUP_DONE = booleanPreferencesKey("setup_completed")
        val KEY_AUTOPLAY = booleanPreferencesKey("autoplay")
        val KEY_SPEED = floatPreferencesKey("playback_speed")
        val KEY_PROGRESS = stringPreferencesKey("watch_progress")
    }
}
