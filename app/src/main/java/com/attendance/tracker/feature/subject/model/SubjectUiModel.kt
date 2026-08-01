package com.attendance.tracker.feature.subject.model

/**
 * UI-layer representation of a study subject, decoupled from database annotations.
 */
data class SubjectUiModel(
    val id: Long,
    val name: String,
    val faculty: String? = null,
    val requiredAttendance: Int = 75,
    val attendanceGoal: Int = 85,
    val color: Int = 0
)
