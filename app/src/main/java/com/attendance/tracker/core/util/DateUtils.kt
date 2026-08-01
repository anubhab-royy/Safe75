package com.attendance.tracker.core.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Utility functions for local date manipulation, parsing, and formatting.
 * Leveraging modern java.time APIs available in API 26+.
 */
object DateUtils {
    private val defaultDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())

    /**
     * Returns the current date as a [LocalDate].
     */
    fun getToday(): LocalDate = LocalDate.now()

    /**
     * Formats a [LocalDate] into a readable string using the specified pattern.
     */
    fun formatLocalDate(date: LocalDate, pattern: String = "dd MMM yyyy"): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            date.format(formatter)
        } catch (e: Exception) {
            date.format(displayDateFormatter)
        }
    }

    /**
     * Returns the capitalized name of the current day of the week (e.g., "MONDAY").
     */
    fun getCurrentDayOfWeekName(): String {
        return getToday().dayOfWeek.name
    }

    /**
     * Parses a string representation of a date into a [LocalDate].
     * Returns null if parsing fails.
     */
    fun parseLocalDate(dateString: String, pattern: String = "yyyy-MM-dd"): LocalDate? {
        return try {
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
            LocalDate.parse(dateString, formatter)
        } catch (e: DateTimeParseException) {
            try {
                LocalDate.parse(dateString, defaultDateFormatter)
            } catch (ex: DateTimeParseException) {
                null
            }
        }
    }
}
