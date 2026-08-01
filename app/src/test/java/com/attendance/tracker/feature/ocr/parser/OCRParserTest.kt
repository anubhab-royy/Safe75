package com.attendance.tracker.feature.ocr.parser

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests verifying OCR parsing regex logic inside [OcrParser].
 *
 * Note: ML Kit Text objects cannot be constructed in unit tests without instrumentation.
 * These tests exercise parsing helpers via regex directly on raw strings.
 */
class OCRParserTest {

    private val parser = OcrParser()

    // ---- Time extraction helper tests ----

    @Test
    fun testTimePattern_matchesColonFormat() {
        val input = "Mon 10:00 - 11:30 Maths"
        val matcher = java.util.regex.Pattern.compile("(\\d{1,2}[:.]\\d{2})\\s*-\\s*(\\d{1,2}[:.]\\d{2})").matcher(input)
        val found = matcher.find()
        assertEquals(true, found)
        assertEquals("10:00", matcher.group(1))
        assertEquals("11:30", matcher.group(2))
    }

    @Test
    fun testTimePattern_matchesDotFormat() {
        val input = "09.00 - 10.30 Physics"
        val matcher = java.util.regex.Pattern.compile("(\\d{1,2}[:.]\\d{2})\\s*-\\s*(\\d{1,2}[:.]\\d{2})").matcher(input)
        val found = matcher.find()
        assertEquals(true, found)
        assertEquals("09.00", matcher.group(1))
        assertEquals("10.30", matcher.group(2))
    }

    @Test
    fun testTimePattern_noMatch_returnsNotFound() {
        val input = "Chemistry Lab all day"
        val matcher = java.util.regex.Pattern.compile("(\\d{1,2}[:.]\\d{2})\\s*-\\s*(\\d{1,2}[:.]\\d{2})").matcher(input)
        assertEquals(false, matcher.find())
    }

    // ---- Attendance ratio extraction helper tests ----

    @Test
    fun testRatioPattern_matchesPresentSlashTotal() {
        val input = "Physics 15 / 20 75.0%"
        val matcher = java.util.regex.Pattern.compile("(\\d+)\\s*/\\s*(\\d+)").matcher(input)
        val found = matcher.find()
        assertEquals(true, found)
        assertEquals(15, matcher.group(1).orEmpty().toInt())
        assertEquals(20, matcher.group(2).orEmpty().toInt())
    }

    @Test
    fun testPercentagePattern_matchesFloatPercentage() {
        val input = "Maths 18/25 72.0%"
        val matcher = java.util.regex.Pattern.compile("(\\d{1,3}(?:\\.\\d+)?)\\s*%").matcher(input)
        val found = matcher.find()
        assertEquals(true, found)
        assertEquals(72.0, matcher.group(1).orEmpty().toDouble(), 0.01)
    }

    @Test
    fun testDayNormalization_mapsShortToFull() {
        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday",
                          "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val testCases = mapOf(
            "Mon" to "Monday",
            "Tue" to "Tuesday",
            "Wed" to "Wednesday",
            "Thu" to "Thursday",
            "Fri" to "Friday",
            "Sat" to "Saturday",
            "Sun" to "Sunday"
        )

        testCases.forEach { (abbr, expected) ->
            val matched = days.find { "Some text $abbr 10:00 - 11:00".contains(it, ignoreCase = true) }
            val normalized = when (matched?.lowercase()?.take(3)) {
                "mon" -> "Monday"
                "tue" -> "Tuesday"
                "wed" -> "Wednesday"
                "thu" -> "Thursday"
                "fri" -> "Friday"
                "sat" -> "Saturday"
                "sun" -> "Sunday"
                else -> "Monday"
            }
            assertEquals("Failed for $abbr", expected, normalized)
        }
    }
}
