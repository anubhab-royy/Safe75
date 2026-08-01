package com.attendance.tracker.core.util

import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Utility functions for local time manipulation, parsing, formatting, and duration calculations.
 * Leveraging modern java.time APIs available in API 26+.
 */
object TimeUtils {
    private val defaultTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val displayTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())

    /**
     * Formats a [LocalTime] into a readable string using the specified pattern.
     */
    fun formatLocalTime(time: LocalTime, pattern: String = "hh:mm a"): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            time.format(formatter)
        } catch (e: Exception) {
            time.format(displayTimeFormatter)
        }
    }

    /**
     * Parses a string representation of time into a [LocalTime].
     * Returns null if parsing fails.
     */
    fun parseLocalTime(timeString: String, pattern: String = "HH:mm"): LocalTime? {
        return try {
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            LocalTime.parse(timeString, formatter)
        } catch (e: DateTimeParseException) {
            try {
                LocalTime.parse(timeString, defaultTimeFormatter)
            } catch (ex: DateTimeParseException) {
                null
            }
        }
    }

    /**
     * Calculates the duration in minutes between [startTime] and [endTime].
     */
    fun calculateDurationInMinutes(startTime: LocalTime, endTime: LocalTime): Long {
        return Duration.between(startTime, endTime).toMinutes()
    }
}
