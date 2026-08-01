package com.attendance.tracker.feature.ocr.model

/**
 * Encapsulates a scanned field value paired with its ML Kit recognition confidence score.
 */
data class OcrField<T>(
    val value: T,
    val confidence: Float // confidence float between 0.0f and 1.0f
)

/**
 * UI representation of a scanned timetable class row.
 */
data class OcrTimetableRow(
    val id: String,
    val subjectName: OcrField<String>,
    val dayOfWeek: OcrField<String>,
    val startTime: OcrField<String>,
    val endTime: OcrField<String>,
    val faculty: OcrField<String?> = OcrField(null, 1.0f),
    val room: OcrField<String?> = OcrField(null, 1.0f)
)

/**
 * UI representation of a scanned existing attendance record row.
 */
data class OcrAttendanceRow(
    val id: String,
    val subjectName: OcrField<String>,
    val presentCount: OcrField<Int>,
    val totalClasses: OcrField<Int>,
    val percentage: OcrField<Double>,
    val matchedSubjectId: Long? = null
)
