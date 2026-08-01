package com.attendance.tracker.feature.semester.model

/**
 * UI-layer representation of a semester config.
 */
data class SemesterUiModel(
    val id: Long,
    val name: String,
    val dateRangeDisplay: String
)
