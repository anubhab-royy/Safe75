package com.attendance.tracker.feature.subject.model

/**
 * UI-layer representation of a study subject, decoupled from database annotations.
 */
data class SubjectUiModel(
    val id: Long,
    val name: String,
    val code: String? = null,
    val creditHours: Int = 0
)
