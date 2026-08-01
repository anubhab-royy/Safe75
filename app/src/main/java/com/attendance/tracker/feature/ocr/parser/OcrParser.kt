package com.attendance.tracker.feature.ocr.parser

import com.attendance.tracker.feature.ocr.model.OcrField
import com.attendance.tracker.feature.ocr.model.OcrTimetableRow
import com.attendance.tracker.feature.ocr.model.OcrAttendanceRow
import com.google.mlkit.vision.text.Text
import java.util.UUID
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Parses raw text structures into structured Timetable and Attendance records.
 */
class OcrParser @Inject constructor() {

    private val timePattern = Pattern.compile("(\\d{1,2}[:.]\\d{2})\\s*-\\s*(\\d{1,2}[:.]\\d{2})")
    private val percentagePattern = Pattern.compile("(\\d{1,3}(?:\\.\\d+)?)\\s*%")
    private val ratioPattern = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)")

    private val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday",
                              "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    /**
     * Extracts structured timetable rows from text blocks.
     */
    fun parseTimetable(text: Text): List<OcrTimetableRow> {
        val rows = mutableListOf<OcrTimetableRow>()

        for (block in text.textBlocks) {
            for (line in block.lines) {
                val lineText = line.text
                val timeMatcher = timePattern.matcher(lineText)
                if (timeMatcher.find()) {
                    val start = timeMatcher.group(1).orEmpty().replace(".", ":")
                    val end = timeMatcher.group(2).orEmpty().replace(".", ":")

                    val matchedDay = days.find { lineText.contains(it, ignoreCase = true) } ?: "Monday"
                    val dayStr = when(matchedDay.lowercase().take(3)) {
                        "mon" -> "Monday"
                        "tue" -> "Tuesday"
                        "wed" -> "Wednesday"
                        "thu" -> "Thursday"
                        "fri" -> "Friday"
                        "sat" -> "Saturday"
                        "sun" -> "Sunday"
                        else -> "Monday"
                    }

                    var cleanText = lineText.replace(timeMatcher.group(0).orEmpty(), "").replace(matchedDay, "")

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
                            subjectName = OcrField(subjectName, confidence),
                            dayOfWeek = OcrField(dayStr, confidence),
                            startTime = OcrField(start, confidence),
                            endTime = OcrField(end, confidence),
                            faculty = OcrField(facultyVal, confidence),
                            room = OcrField(roomVal, confidence)
                        )
                    )
                }
            }
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
