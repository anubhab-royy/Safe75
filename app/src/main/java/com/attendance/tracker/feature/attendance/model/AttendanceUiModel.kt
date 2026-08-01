package com.attendance.tracker.feature.attendance.model

/**
 * UI-layer representation of an attendance record, enriched with subject metadata and timeframes.
 */
data class AttendanceUiModel(
    val id: Long,
    val subjectId: Long,
    val scheduleId: Long,
    val date: String,
    val status: String,
    val remarks: String? = null,
    val subjectName: String = "",
    val subjectColor: Int = 0xFF9E9E9E.toInt(),
    val timeRange: String = ""
)
