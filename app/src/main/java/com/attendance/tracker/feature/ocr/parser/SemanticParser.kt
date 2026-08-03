package com.attendance.tracker.feature.ocr.parser

import com.attendance.tracker.feature.ocr.recognition.RecognizedCell
import com.attendance.tracker.feature.ocr.validation.ValidationEngine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton
import java.util.regex.Pattern

@Serializable
data class ParsedSubjectClass(
    val day: String,
    val start: String,
    val end: String,
    val subject: String,
    val faculty: String?,
    val facultyName: String?,
    val lab: Boolean
)

@Serializable
data class ParsedTimetableResult(
    val subjects: List<ParsedSubjectClass>
)

/**
 * Parses grid cell OCR texts into strongly typed timetable data.
 */
@Singleton
class SemanticParser @Inject constructor(
    private val validationEngine: ValidationEngine,
    private val headerInterpreter: HeaderInterpreter
) {

    private val daysOfWeek = setOf(
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    )

    private val json = Json { prettyPrint = true }

    // Faculty code abbreviation to full name lookup mapping
    private val facultyLookup = mapOf(
        "TB" to "Prof. Tulika Bhattacharya",
        "ABG" to "Prof. Abir Ghosh",
        "AS" to "Prof. Ayanika Samanta",
        "ANM" to "Prof. Ankur Mondal",
        "SB" to "Dr. Samik Basu",
        "RK" to "Prof. Ravish Kumar",
        "TS" to "Prof. Tanushree Sarkar"
    )

    /**
     * Parses a list of recognized cells and extracts structured subject routines.
     * Excludes special slots like Recess/Lunch and library hours.
     */
    fun parse(cells: List<RecognizedCell>): ParsedTimetableResult {
        // 1. Identify Header Row (contains the time slot headers)
        val headerRowIndex = cells.groupBy { it.startRow }
            .entries
            .maxByOrNull { entry ->
                entry.value.count { cell ->
                    val norm = headerInterpreter.normalize(cell.text)
                    headerInterpreter.extractTimeRange(norm) != null
                }
            }?.key ?: 0

        // 2. Identify Day Column (contains the days of the week)
        val dayColIndex = cells.groupBy { it.startCol }
            .entries
            .maxByOrNull { entry ->
                entry.value.count { cell ->
                    val clean = validationEngine.cleanDay(cell.text)
                    daysOfWeek.contains(clean)
                }
            }?.key ?: 0

        // 3. Extract time slots for columns from the header row
        val timeSlots = HashMap<Int, Pair<String, String>>()
        cells.filter { it.startRow == headerRowIndex }.forEach { cell ->
            val normalized = headerInterpreter.normalize(cell.text)
            val range = headerInterpreter.extractTimeRange(normalized)
            if (range != null) {
                // Assign the times slot to each column spanned by this cell
                for (c in 0 until cell.colSpan) {
                    timeSlots[cell.startCol + c] = range
                }
            }
        }

        // 4. Extract weekday mappings for rows from the day column
        val rowDays = HashMap<Int, String>()
        cells.filter { it.startCol == dayColIndex }.forEach { cell ->
            val cleanDay = validationEngine.cleanDay(cell.text)
            if (daysOfWeek.contains(cleanDay)) {
                for (r in 0 until cell.rowSpan) {
                    rowDays[cell.startRow + r] = cleanDay
                }
            }
        }

        // 5. Parse the content cells
        val parsedClasses = ArrayList<ParsedSubjectClass>()
        val specialKeywords = setOf("recess", "lunch", "library", "recreation", "gap", "break")

        val contentCells = cells.filter {
            it.startRow != headerRowIndex &&
            it.startCol != dayColIndex &&
            it.text.isNotBlank()
        }

        // Debug outputs block
        println("--- SEMANTIC PARSER DEBUG ---")
        println("Detected Header Row: $headerRowIndex")
        println("Detected Day Column: $dayColIndex")
        println("Detected Column Mapping:")
        timeSlots.forEach { (col, range) ->
            println("  Column $col -> ${range.first} - ${range.second}")
        }
        println("Detected Row Mapping:")
        rowDays.forEach { (row, day) ->
            println("  Row $row -> $day")
        }

        for (cell in contentCells) {
            val text = cell.text.trim()
            val lowerText = text.lowercase()

            // Skip recess/library hour cells
            if (specialKeywords.any { lowerText.contains(it) }) {
                println("  Cell (Row ${cell.startRow} Col ${cell.startCol}) -> colSpan=${cell.colSpan}, text='$text' -> [SKIPPED (Special Keyword)]")
                continue
            }

            val day = rowDays[cell.startRow]
            if (day == null) {
                println("  Cell (Row ${cell.startRow} Col ${cell.startCol}) -> colSpan=${cell.colSpan}, text='$text' -> [SKIPPED (No matching day)]")
                continue
            }

            // Determine start and end times based on columns spanned
            val colStart = cell.startCol
            val colEnd = cell.startCol + cell.colSpan - 1

            val startTime = timeSlots[colStart]?.first
            val endTime = timeSlots[colEnd]?.second ?: timeSlots[colStart]?.second

            if (startTime == null || endTime == null) {
                println("  Cell (Row ${cell.startRow} Col ${cell.startCol}) -> colSpan=${cell.colSpan}, text='$text' -> [SKIPPED (Missing time ranges: colStart=$colStart, colEnd=$colEnd)]")
                continue
            }

            val cleanedText = validationEngine.cleanSubjectText(text)

            // Extract Faculty Abbreviation in parentheses, e.g. "(ANM)" or "(SB)"
            val facultyRegex = Regex("\\(([A-Za-z]{2,4})\\)")
            val facultyMatch = facultyRegex.find(cleanedText)
            val faculty = facultyMatch?.groupValues?.get(1)?.uppercase()
            val facultyName = faculty?.let { facultyLookup[it] }

            val subjectOnly = cleanedText.replace(facultyRegex, "").trim()
                .replace(Regex("\\s+"), " ")

            val isLab = subjectOnly.contains("lab", ignoreCase = true)

            println("  Cell (Row ${cell.startRow} Col ${cell.startCol}) -> " +
                    "colSpan=${cell.colSpan}, rect=(${cell.rect.x}, ${cell.rect.y}, ${cell.rect.width}, ${cell.rect.height}), " +
                    "text='$text' -> Day=$day, Start=$startTime, End=$endTime, Subject='$subjectOnly', FacultyCode=$faculty, FacultyName=$facultyName, Lab=$isLab [PARSED]")

            parsedClasses.add(
                ParsedSubjectClass(
                    day = day,
                    start = startTime,
                    end = endTime,
                    subject = subjectOnly,
                    faculty = faculty,
                    facultyName = facultyName,
                    lab = isLab
                )
            )
        }
        println("-----------------------------")

        return ParsedTimetableResult(subjects = parsedClasses)
    }

    /**
     * Converts a parsed routine into a formatted JSON string.
     */
    fun toJson(result: ParsedTimetableResult): String {
        return json.encodeToString(result)
    }
}
