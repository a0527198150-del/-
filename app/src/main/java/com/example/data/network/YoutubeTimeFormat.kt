package com.example.data.network

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/** Date/duration helpers shared by the API, RSS and scraping code paths. */
object YoutubeTimeFormat {

    /** Parses an ISO-8601 timestamp such as `2024-03-11T08:15:00Z` into epoch millis. */
    fun parseIsoToMillis(raw: String): Long {
        if (raw.length < 10) return 0L
        return try {
            val year = raw.substring(0, 4).toInt()
            val month = raw.substring(5, 7).toInt()
            val day = raw.substring(8, 10).toInt()
            var hour = 0
            var minute = 0
            var second = 0
            if (raw.length >= 19 && raw[10] == 'T') {
                hour = raw.substring(11, 13).toInt()
                minute = raw.substring(14, 16).toInt()
                second = raw.substring(17, 19).toInt()
            }
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US)
            calendar.clear()
            calendar.set(year, month - 1, day, hour, minute, second)
            calendar.timeInMillis - offsetMillis(raw)
        } catch (error: Exception) {
            0L
        }
    }

    /** Honours a trailing `+02:00` / `-05:00` offset when one is present. */
    private fun offsetMillis(raw: String): Long {
        // Only a full `...THH:mm:ss±HH:mm` timestamp carries an offset; a bare
        // date would otherwise have its own dashes read as one.
        if (raw.length < 25 || raw[10] != 'T') return 0L
        val tail = raw.substring(raw.length - 6)
        if (tail[3] != ':') return 0L
        val sign = when (tail.firstOrNull()) {
            '+' -> 1
            '-' -> -1
            else -> return 0L
        }
        return try {
            val hours = tail.substring(1, 3).toInt()
            val minutes = tail.substring(4, 6).toInt()
            sign * TimeUnit.MINUTES.toMillis((hours * 60 + minutes).toLong())
        } catch (error: Exception) {
            0L
        }
    }

    /** `PT1H2M10S` -> 3730 seconds. Returns 0 for live streams and malformed input. */
    fun parseIsoDuration(raw: String): Int {
        if (!raw.startsWith("PT")) return 0
        var total = 0
        var number = 0
        for (character in raw.substring(2)) {
            when {
                character.isDigit() -> number = number * 10 + (character - '0')
                character == 'H' -> { total += number * 3600; number = 0 }
                character == 'M' -> { total += number * 60; number = 0 }
                character == 'S' -> { total += number; number = 0 }
                else -> number = 0
            }
        }
        return total
    }

    /** `3730` -> `1:02:10`. */
    fun formatClock(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        val seconds = safe % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    /** Hebrew relative date, e.g. "לפני 3 ימים". */
    fun relativeHebrew(millis: Long): String {
        if (millis <= 0L) return ""
        val delta = System.currentTimeMillis() - millis
        if (delta < 0) return "חדש"
        val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
        val hours = TimeUnit.MILLISECONDS.toHours(delta)
        val days = TimeUnit.MILLISECONDS.toDays(delta)
        return when {
            minutes < 1L -> "הרגע"
            minutes < 60L -> "לפני $minutes דקות"
            hours < 2L -> "לפני שעה"
            hours < 24L -> "לפני $hours שעות"
            days < 2L -> "אתמול"
            days < 7L -> "לפני $days ימים"
            days < 14L -> "לפני שבוע"
            days < 31L -> "לפני ${days / 7} שבועות"
            days < 60L -> "לפני חודש"
            days < 365L -> "לפני ${days / 30} חודשים"
            days < 730L -> "לפני שנה"
            else -> "לפני ${days / 365} שנים"
        }
    }

    /** `dd/MM/yyyy`, used when only a date string is available. */
    fun formatDate(millis: Long): String {
        if (millis <= 0L) return ""
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        return String.format(
            Locale.US,
            "%02d/%02d/%d",
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.YEAR),
        )
    }
}
