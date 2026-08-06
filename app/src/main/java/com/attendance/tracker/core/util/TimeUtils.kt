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
    // Data format used for OCR parsing, storage, and backup round-trips.
    // Kept locale-independent (Locale.ROOT) to avoid digit/format drift in
    // locales that use non-ASCII digits.
    private val defaultTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

    /**
     * Formats a [LocalTime] into a readable string using the specified pattern.
     */
    fun formatLocalTime(time: LocalTime, pattern: String = "hh:mm a"): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            time.format(formatter)
        } catch (e: Exception) {
            time.format(displayTimeFormatter())
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

    /**
     * Returns a locale-aware formatter for displaying times. Created on demand
     * so display follows the current locale even if it changes at runtime.
     */
    fun displayTimeFormatter(): DateTimeFormatter =
        DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
}
