package com.attendance.tracker.feature.ocr.validation

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates and auto-corrects common OCR mistakes using configurable dictionaries.
 * Corrects day spelling errors, unmatched brackets, and subject abbreviations.
 */
@Singleton
class ValidationEngine @Inject constructor() {

    private val dayCorrections = mapOf(
        "mondav" to "Monday",
        "tuesdav" to "Tuesday",
        "wednesdav" to "Wednesday",
        "thursdav" to "Thursday",
        "fridav" to "Friday",
        "saturdav" to "Saturday",
        "sundav" to "Sunday",
        "mon" to "Monday",
        "tue" to "Tuesday",
        "wed" to "Wednesday",
        "thu" to "Thursday",
        "fri" to "Friday",
        "sat" to "Saturday",
        "sun" to "Sunday"
    )

    private val subjectCorrections = mapOf(
        "computor networks" to "Computer Networks",
        "computor networks lab" to "Computer Networks Lab",
        "oop using javaa" to "OOP using JAVA",
        "oop using javaa lab" to "OOP using JAVA LAB",
        "software engineering" to "Software Engineering",
        "theory of computation" to "Theory of Computation",
        "introduction to ai" to "Introduction to AI",
        "ai lab" to "AI Lab"
    )

    /**
     * Standardizes recognized day names.
     */
    fun cleanDay(day: String): String {
        val normalized = day.trim().lowercase()
        return dayCorrections[normalized] ?: day.trim().replaceFirstChar { it.uppercase() }
    }

    /**
     * Cleans OCR spelling anomalies and normalizes spacing.
     */
    fun cleanSubjectText(text: String): String {
        var clean = text.trim().replace(Regex("\\s+"), " ")

        // Fix unmatched brackets for faculty abbreviations, e.g. "SB)" -> "(SB)", "(ANM" -> "(ANM)"
        val unmatchedClosing = Regex("(?<!\\()\\b([A-Z]{2,3})\\)")
        clean = unmatchedClosing.replace(clean) { matchResult ->
            "(${matchResult.groupValues[1]})"
        }

        val unmatchedOpening = Regex("\\(([A-Z]{2,3})\\b(?!\\))")
        clean = unmatchedOpening.replace(clean) { matchResult ->
            "(${matchResult.groupValues[1]})"
        }

        // Apply known subject corrections
        val lower = clean.lowercase()
        for ((wrong, correct) in subjectCorrections) {
            if (lower.contains(wrong)) {
                val regex = Regex("(?i)\\b$wrong\\b")
                clean = clean.replace(regex, correct)
            }
        }

        return clean
    }
}
