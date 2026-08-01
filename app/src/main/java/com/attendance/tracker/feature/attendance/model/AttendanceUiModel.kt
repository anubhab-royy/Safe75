package com.attendance.tracker.feature.attendance.model

/**
 * UI-layer representation of an attendance record.
 */
data class AttendanceUiModel(
    val id: Long,
    val subjectId: Long,
    val date: String,
    val status: String,
    val note: String? = null
)
