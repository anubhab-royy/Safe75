package com.attendance.tracker.feature.ocr.parser

import android.graphics.Rect
import com.google.mlkit.vision.text.Text
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Unit tests verifying OCR parsing regex logic inside [OcrParser].
 *
 * Note: ML Kit Text objects cannot be constructed in unit tests without instrumentation.
 * These tests exercise parsing helpers via regex directly on raw strings.
 */
class OCRParserTest {

    private val parser = OcrParser()

    private val topMap = mutableMapOf<Rect, Int>()
    private val bottomMap = mutableMapOf<Rect, Int>()
    private val leftMap = mutableMapOf<Rect, Int>()
    private val rightMap = mutableMapOf<Rect, Int>()

    private val testRectBridge = object : RectBridge {
        override fun getTop(rect: Rect) = topMap[rect] ?: 0
        override fun getBottom(rect: Rect) = bottomMap[rect] ?: 0
        override fun getLeft(rect: Rect) = leftMap[rect] ?: 0
        override fun getRight(rect: Rect) = rightMap[rect] ?: 0
    }

    private fun mockLine(
        text: String,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        confidence: Float = 0.9f
    ): Text.Line {
        val line = mock(Text.Line::class.java)
        `when`(line.text).thenReturn(text)
        
        val rect = mock(Rect::class.java)
        `when`(line.boundingBox).thenReturn(rect)
        topMap[rect] = top
        bottomMap[rect] = bottom
        leftMap[rect] = left
        rightMap[rect] = right
        
        val elem = mock(Text.Element::class.java)
        `when`(elem.confidence).thenReturn(confidence)
        `when`(line.elements).thenReturn(listOf(elem))
        
        return line
    }

    private fun mockLegacyLine(text: String, confidence: Float = 0.9f): Text.Line {
        val line = mock(Text.Line::class.java)
        `when`(line.text).thenReturn(text)
        `when`(line.boundingBox).thenReturn(null)
        
        val elem = mock(Text.Element::class.java)
        `when`(elem.confidence).thenReturn(confidence)
        `when`(line.elements).thenReturn(listOf(elem))
        
        return line
    }

    private fun mockText(vararg blocks: List<Text.Line>): Text {
        val mockText = mock(Text::class.java)
        val textBlocks = blocks.map { lines ->
            val block = mock(Text.TextBlock::class.java)
            `when`(block.lines).thenReturn(lines)
            block
        }
        `when`(mockText.textBlocks).thenReturn(textBlocks)
        return mockText
    }

    // ---- Time extraction helper tests ----

    @Test
    fun testTimePattern_matchesColonFormat() {
        val input = "Mon 10:00 - 11:30 Maths"
        val matcher = parser.timePattern.matcher(input)
        val found = matcher.find()
        assertEquals(true, found)
        assertEquals("10:00", matcher.group(1))
        assertEquals("11:30", matcher.group(2))
    }

    @Test
    fun testTimePattern_matchesDotFormat() {
        val input = "09.00 - 10.30 Physics"
        val matcher = parser.timePattern.matcher(input)
        val found = matcher.find()
        assertEquals(true, found)
        assertEquals("09.00", matcher.group(1))
        assertEquals("10.30", matcher.group(2))
    }

    @Test
    fun testTimePattern_matchesUnicodeEnDash() {
        val input = "Mon 10:00 – 11:30 Maths"
        val matcher = parser.timePattern.matcher(input)
        val found = matcher.find()
        assertEquals(true, found)
        assertEquals("10:00", matcher.group(1))
        assertEquals("11:30", matcher.group(2))
    }

    @Test
    fun testTimePattern_matchesUnicodeEmDash() {
        val input = "Mon 10:00 — 11:30 Maths"
        val matcher = parser.timePattern.matcher(input)
        val found = matcher.find()
        assertEquals(true, found)
        assertEquals("10:00", matcher.group(1))
        assertEquals("11:30", matcher.group(2))
    }

    @Test
    fun testTimePattern_matchesUnicodeMinus() {
        val input = "Mon 10:00 − 11:30 Maths"
        val matcher = parser.timePattern.matcher(input)
        val found = matcher.find()
        assertEquals(true, found)
        assertEquals("10:00", matcher.group(1))
        assertEquals("11:30", matcher.group(2))
    }

    @Test
    fun testTimePattern_noMatch_returnsNotFound() {
        val input = "Chemistry Lab all day"
        val matcher = parser.timePattern.matcher(input)
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

    @Test
    fun testParseTimetable_spatialRowBasedGrid() {
        parser.rectBridge = testRectBridge
        topMap.clear()
        bottomMap.clear()
        leftMap.clear()
        rightMap.clear()

        // Row-based grid:
        // Column 0: Time
        // Column 1: Mon
        // Row 1: Time slot 09:00 - 10:00 -> Subject CS101
        // Row 2: Time slot 10:00 - 11:00 -> Subject CS104
        
        val timeHeader = mockLine("Time", 0, 0, 150, 30)
        val t1 = mockLine("09:00 - 10:00", 0, 40, 150, 70)
        val t2 = mockLine("10:00 - 11:00", 0, 80, 150, 110)
        val timeColumn = listOf(timeHeader, t1, t2)

        val dayHeader = mockLine("Mon", 160, 0, 240, 30)
        val s1 = mockLine("CS101", 160, 40, 240, 70)
        val s2 = mockLine("CS104", 160, 80, 240, 110)
        val dayColumn = listOf(dayHeader, s1, s2)

        val text = mockText(timeColumn, dayColumn)

        val result = parser.parseTimetable(text)

        assertEquals(2, result.size)
        
        val row0 = result.find { it.startTime.value == "09:00" }
        val row1 = result.find { it.startTime.value == "10:00" }

        assertEquals(true, row0 != null)
        assertEquals("CS101", row0?.subjectName?.value)
        assertEquals("Monday", row0?.dayOfWeek?.value)
        assertEquals("10:00", row0?.endTime?.value)

        assertEquals(true, row1 != null)
        assertEquals("CS104", row1?.subjectName?.value)
        assertEquals("Monday", row1?.dayOfWeek?.value)
        assertEquals("11:00", row1?.endTime?.value)
    }

    @Test
    fun testParseTimetable_spatialColumnBasedGrid() {
        parser.rectBridge = testRectBridge
        topMap.clear()
        bottomMap.clear()
        leftMap.clear()
        rightMap.clear()

        // Column-based grid:
        // Row 0: Header with Time slots (X columns)
        // Row 1: Monday row (Y=40..70)
        
        val timeHeader = mockLine("Day", 0, 0, 100, 30)
        val t1 = mockLine("09:00 - 10:00", 110, 0, 250, 30)
        val t2 = mockLine("10:00 - 11:00", 260, 0, 400, 30)
        val headerRow = listOf(timeHeader, t1, t2)

        val monHeader = mockLine("Mon", 0, 40, 100, 70)
        val s1 = mockLine("CS101", 110, 40, 250, 70)
        val s2 = mockLine("CS104", 260, 40, 400, 70)
        val monRow = listOf(monHeader, s1, s2)

        val text = mockText(headerRow, monRow)

        val result = parser.parseTimetable(text)

        assertEquals(2, result.size)
        
        val row0 = result.find { it.startTime.value == "09:00" }
        val row1 = result.find { it.startTime.value == "10:00" }

        assertEquals(true, row0 != null)
        assertEquals("CS101", row0?.subjectName?.value)
        assertEquals("Monday", row0?.dayOfWeek?.value)

        assertEquals(true, row1 != null)
        assertEquals("CS104", row1?.subjectName?.value)
        assertEquals("Monday", row1?.dayOfWeek?.value)
    }

    @Test
    fun testParseTimetable_fallbackAndLegacy() {
        parser.rectBridge = testRectBridge
        topMap.clear()
        bottomMap.clear()
        leftMap.clear()
        rightMap.clear()

        val legacyLine = mockLegacyLine("Mon 09:00 - 10:00 CS101")
        val spatialLine = mockLine("Tue 10:00 - 11:00 CS104", 0, 0, 300, 30)
        
        val text = mockText(listOf(legacyLine, spatialLine))

        val result = parser.parseTimetable(text)

        assertEquals(2, result.size)
        
        val row0 = result.find { it.startTime.value == "09:00" }
        val row1 = result.find { it.startTime.value == "10:00" }

        assertEquals(true, row0 != null)
        assertEquals("CS101", row0?.subjectName?.value)
        assertEquals("Monday", row0?.dayOfWeek?.value)

        assertEquals(true, row1 != null)
        assertEquals("CS104", row1?.subjectName?.value)
        assertEquals("Tuesday", row1?.dayOfWeek?.value)
    }
}
