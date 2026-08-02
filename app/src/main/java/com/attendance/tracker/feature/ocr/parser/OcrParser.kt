package com.attendance.tracker.feature.ocr.parser

import android.graphics.Rect
import com.attendance.tracker.feature.ocr.model.OcrField
import com.attendance.tracker.feature.ocr.model.OcrTimetableRow
import com.attendance.tracker.feature.ocr.model.OcrAttendanceRow
import com.google.mlkit.vision.text.Text
import java.util.UUID
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Interface to bridge access to android.graphics.Rect fields in unit tests on standard JVM.
 */
internal interface RectBridge {
    fun getTop(rect: Rect): Int
    fun getBottom(rect: Rect): Int
    fun getLeft(rect: Rect): Int
    fun getRight(rect: Rect): Int
}

private class DefaultRectBridge : RectBridge {
    override fun getTop(rect: Rect) = rect.top
    override fun getBottom(rect: Rect) = rect.bottom
    override fun getLeft(rect: Rect) = rect.left
    override fun getRight(rect: Rect) = rect.right
}

/**
 * Parses raw text structures into structured Timetable and Attendance records.
 */
class OcrParser @Inject constructor() {

    internal var rectBridge: RectBridge = DefaultRectBridge()

    internal val timePattern = Pattern.compile("(\\d{1,2}[:.]\\d{2})\\s*[-–—−]\\s*(\\d{1,2}[:.]\\d{2})")
    private val percentagePattern = Pattern.compile("(\\d{1,3}(?:\\.\\d+)?)\\s*%")
    private val ratioPattern = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)")

    private val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday",
                              "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    private data class ParsedInfo(
        val subjectName: String,
        val faculty: String?,
        val room: String?
    )

    private fun normalizeDay(dayName: String): String {
        return when (dayName.lowercase().take(3)) {
            "mon" -> "Monday"
            "tue" -> "Tuesday"
            "wed" -> "Wednesday"
            "thu" -> "Thursday"
            "fri" -> "Friday"
            "sat" -> "Saturday"
            "sun" -> "Sunday"
            else -> "Monday"
        }
    }

    private fun extractFieldsFromText(text: String): ParsedInfo {
        var cleanText = text
        
        val roomRegex = Regex("(?i)room\\s*(\\w+)|r-\\d+")
        val roomMatch = roomRegex.find(cleanText)
        val roomVal = roomMatch?.value?.trim()
        if (roomMatch != null) {
            cleanText = cleanText.replace(roomMatch.value, "")
        }

        val facultyRegex = Regex("(?i)dr\\.\\s*\\w+|prof\\.\\s*\\w+")
        val facultyMatch = facultyRegex.find(cleanText)
        val facultyVal = facultyMatch?.value?.trim()
        if (facultyMatch != null) {
            cleanText = cleanText.replace(facultyMatch.value, "")
        }

        val subjectName = cleanText.trim().replace(Regex("\\s+"), " ").takeIf { it.isNotBlank() } ?: "Subject"
        
        return ParsedInfo(subjectName, facultyVal, roomVal)
    }

    private fun parseLinearLine(line: Text.Line, rows: MutableList<OcrTimetableRow>) {
        val lineText = line.text
        val timeMatcher = timePattern.matcher(lineText)
        if (timeMatcher.find()) {
            val start = timeMatcher.group(1).orEmpty().replace(".", ":")
            val end = timeMatcher.group(2).orEmpty().replace(".", ":")

            val matchedDay = days.find { lineText.contains(it, ignoreCase = true) } ?: "Monday"
            val dayStr = normalizeDay(matchedDay)

            val cleanText = lineText.replace(timeMatcher.group(0).orEmpty(), "").replace(matchedDay, "")
            val parsedInfo = extractFieldsFromText(cleanText)

            var sumConfidence = 0.0f
            var count = 0
            line.elements.forEach {
                sumConfidence += it.confidence
                count++
            }
            val confidence = if (count > 0) sumConfidence / count else 0.85f

            rows.add(
                OcrTimetableRow(
                    id = UUID.randomUUID().toString(),
                    subjectName = OcrField(parsedInfo.subjectName, confidence),
                    dayOfWeek = OcrField(dayStr, confidence),
                    startTime = OcrField(start, confidence),
                    endTime = OcrField(end, confidence),
                    faculty = OcrField(parsedInfo.faculty, confidence),
                    room = OcrField(parsedInfo.room, confidence)
                )
            )
        }
    }

    private fun overlap(min1: Int, max1: Int, min2: Int, max2: Int): Int {
        return maxOf(0, minOf(max1, max2) - maxOf(min1, min2))
    }

    private fun overlapsVertically(rect1: Rect, rect2: Rect): Boolean {
        val top1 = rectBridge.getTop(rect1)
        val bottom1 = rectBridge.getBottom(rect1)
        val top2 = rectBridge.getTop(rect2)
        val bottom2 = rectBridge.getBottom(rect2)
        
        val hOverlap = overlap(top1, bottom1, top2, bottom2)
        val minHeight = minOf(bottom1 - top1, bottom2 - top2)
        return minHeight > 0 && hOverlap.toFloat() / minHeight > 0.5f
    }

    private fun overlapsHorizontally(rect1: Rect, rect2: Rect): Boolean {
        val left1 = rectBridge.getLeft(rect1)
        val right1 = rectBridge.getRight(rect1)
        val left2 = rectBridge.getLeft(rect2)
        val right2 = rectBridge.getRight(rect2)
        
        val wOverlap = overlap(left1, right1, left2, right2)
        val minWidth = minOf(right1 - left1, right2 - left2)
        return minWidth > 0 && wOverlap.toFloat() / minWidth > 0.5f
    }

    /**
     * Extracts structured timetable rows from text blocks.
     */
    fun parseTimetable(text: Text): List<OcrTimetableRow> {
        val rows = mutableListOf<OcrTimetableRow>()

        val allLines = text.textBlocks.flatMap { it.lines }
        
        val spatialLines = allLines.filter { it.boundingBox != null }
        val legacyLines = allLines.filter { it.boundingBox == null }

        val timeLines = spatialLines.filter { timePattern.matcher(it.text).find() }
        val dayLines = spatialLines.filter { line ->
            days.any { line.text.contains(it, ignoreCase = true) } && !timeLines.contains(line)
        }
        val contentLines = spatialLines.filter { !timeLines.contains(it) && !dayLines.contains(it) }

        val matchedTimeLines = mutableSetOf<Text.Line>()

        for (timeLine in timeLines) {
            val timeBox = timeLine.boundingBox ?: continue
            val timeMatcher = timePattern.matcher(timeLine.text)
            if (!timeMatcher.find()) continue
            val start = timeMatcher.group(1).orEmpty().replace(".", ":")
            val end = timeMatcher.group(2).orEmpty().replace(".", ":")

            for (dayLine in dayLines) {
                val dayBox = dayLine.boundingBox ?: continue
                val matchedDay = days.find { dayLine.text.contains(it, ignoreCase = true) } ?: continue
                val dayStr = normalizeDay(matchedDay)

                val alignedContents = contentLines.filter { cLine ->
                    val cBox = cLine.boundingBox
                    if (cBox == null) false else {
                        val rowAligned = overlapsVertically(cBox, timeBox) && overlapsHorizontally(cBox, dayBox)
                        val colAligned = overlapsHorizontally(cBox, timeBox) && overlapsVertically(cBox, dayBox)
                        
                        val timeTop = rectBridge.getTop(timeBox)
                        val dayTop = rectBridge.getTop(dayBox)
                        
                        if (dayTop < timeTop) {
                            rowAligned
                        } else if (timeTop < dayTop) {
                            colAligned
                        } else {
                            val timeLeft = rectBridge.getLeft(timeBox)
                            val dayLeft = rectBridge.getLeft(dayBox)
                            if (timeLeft < dayLeft) rowAligned else colAligned
                        }
                    }
                }

                if (alignedContents.isNotEmpty()) {
                    matchedTimeLines.add(timeLine)

                    val combinedText = alignedContents.joinToString(" ") { it.text }
                    val parsedInfo = extractFieldsFromText(combinedText)

                    var sumConfidence = 0.0f
                    var count = 0
                    alignedContents.forEach { cLine ->
                        cLine.elements.forEach { elem ->
                            sumConfidence += elem.confidence
                            count++
                        }
                    }
                    val confidence = if (count > 0) sumConfidence / count else 0.85f

                    rows.add(
                        OcrTimetableRow(
                            id = UUID.randomUUID().toString(),
                            subjectName = OcrField(parsedInfo.subjectName, confidence),
                            dayOfWeek = OcrField(dayStr, confidence),
                            startTime = OcrField(start, confidence),
                            endTime = OcrField(end, confidence),
                            faculty = OcrField(parsedInfo.faculty, confidence),
                            room = OcrField(parsedInfo.room, confidence)
                        )
                    )
                }
            }
        }

        val unmatchedTimeLines = timeLines.filter { !matchedTimeLines.contains(it) }
        for (line in unmatchedTimeLines) {
            parseLinearLine(line, rows)
        }

        for (line in legacyLines) {
            parseLinearLine(line, rows)
        }

        return rows
    }

    /**
     * Extracts structured ERP attendance rows from text blocks.
     */
    fun parseAttendance(text: Text): List<OcrAttendanceRow> {
        val rows = mutableListOf<OcrAttendanceRow>()

        for (block in text.textBlocks) {
            for (line in block.lines) {
                val lineText = line.text
                val ratioMatcher = ratioPattern.matcher(lineText)
                val pctMatcher = percentagePattern.matcher(lineText)

                if (ratioMatcher.find()) {
                    val present = ratioMatcher.group(1).orEmpty().toIntOrNull() ?: 0
                    val total = ratioMatcher.group(2).orEmpty().toIntOrNull() ?: 0

                    val pct = if (pctMatcher.find()) {
                        pctMatcher.group(1).orEmpty().toDoubleOrNull() ?: 0.0
                    } else {
                        if (total > 0) (present.toDouble() / total.toDouble()) * 100.0 else 100.0
                    }

                    var cleanText = lineText.replace(ratioMatcher.group(0).orEmpty(), "")
                    if (pctMatcher.find()) {
                        cleanText = cleanText.replace(pctMatcher.group(0).orEmpty(), "")
                    }

                    val subjectName = cleanText.trim().replace(Regex("\\s+"), " ").takeIf { it.isNotBlank() } ?: "Subject"

                    var sumConfidence = 0.0f
                    var count = 0
                    line.elements.forEach {
                        sumConfidence += it.confidence
                        count++
                    }
                    val confidence = if (count > 0) sumConfidence / count else 0.85f

                    rows.add(
                        OcrAttendanceRow(
                            id = UUID.randomUUID().toString(),
                            subjectName = OcrField(subjectName, confidence),
                            presentCount = OcrField(present, confidence),
                            totalClasses = OcrField(total, confidence),
                            percentage = OcrField(pct, confidence)
                        )
                    )
                }
            }
        }
        return rows
    }
}
