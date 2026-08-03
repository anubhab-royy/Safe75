package com.attendance.tracker.feature.ocr.parser

import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HeaderInterpreter @Inject constructor() {

    private val timePairPattern = Pattern.compile(
        "(\\d{1,2}:\\d{2})(am|pm)?-(\\d{1,2}:\\d{2})(am|pm)?",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Aggressively normalizes header cell OCR text.
     * Handles wrapped digits, whitespaces, dots, missing colons.
     */
    fun normalize(text: String): String {
        // 1. Remove all whitespaces, tabs, and newline wraps
        var clean = text.replace(Regex("\\s+"), "")

        // 2. Normalize dots between digits to colons (e.g. 12.50 -> 12:50)
        val timeDotRegex = Regex("(\\d{1,2})\\.(\\d{2})")
        clean = timeDotRegex.replace(clean) { matchResult ->
            "${matchResult.groupValues[1]}:${matchResult.groupValues[2]}"
        }

        // 3. Normalize all dashes/hyphens/en-dashes/em-dashes to a simple '-'
        clean = clean.replace(Regex("[-–—−]"), "-")

        // 4. Handle cases like "1250pm" -> "12:50pm" (insert colon if missing)
        val noColonRange = Regex("(\\d{1,2})(\\d{2})(am|pm)?-(\\d{1,2})(\\d{2})(am|pm)?", RegexOption.IGNORE_CASE)
        clean = noColonRange.replace(clean) { matchResult ->
            val h1 = matchResult.groupValues[1]
            val m1 = matchResult.groupValues[2]
            val ampm1 = matchResult.groupValues[3]
            val h2 = matchResult.groupValues[4]
            val m2 = matchResult.groupValues[5]
            val ampm2 = matchResult.groupValues[6]
            "$h1:$m1$ampm1-$h2:$m2$ampm2"
        }

        val noColonSuffix = Regex("(\\d{1,2})(\\d{2})(am|pm)", RegexOption.IGNORE_CASE)
        clean = noColonSuffix.replace(clean) { matchResult ->
            val h = matchResult.groupValues[1]
            val m = matchResult.groupValues[2]
            val ampm = matchResult.groupValues[3]
            "$h:$m$ampm"
        }

        return clean
    }

    /**
     * Extracts a start and end time from a normalized header cell.
     * Returns Pair(start24Hour, end24Hour) or null if no valid time range is found.
     */
    fun extractTimeRange(normalizedText: String): Pair<String, String>? {
        val matcher = timePairPattern.matcher(normalizedText)
        if (matcher.find()) {
            val startStr = matcher.group(1).orEmpty()
            val startAmPm = matcher.group(2)
            val endStr = matcher.group(3).orEmpty()
            val endAmPm = matcher.group(4)

            // Determine PM context: if either is PM, or if the times naturally fall in afternoon (hour < 9)
            val isPm = (startAmPm != null && startAmPm.lowercase() == "pm") ||
                       (endAmPm != null && endAmPm.lowercase() == "pm")

            val cleanStart = normalizeTo24Hour(startStr, isPm)
            val cleanEnd = normalizeTo24Hour(endStr, isPm)
            return Pair(cleanStart, cleanEnd)
        }
        return null
    }

    private fun normalizeTo24Hour(timeStr: String, isPmContext: Boolean): String {
        val clean = timeStr.replace(".", ":")
        val match = Regex("(\\d{1,2}):(\\d{2})").find(clean) ?: return "09:00"
        var hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].toInt()

        if (isPmContext && hour < 12) {
            hour += 12
        } else if (!isPmContext && hour < 9) { // Smart heuristic for afternoon if am/pm is missing
            hour += 12
        }

        return String.format("%02d:%02d", hour, minute)
    }
}
