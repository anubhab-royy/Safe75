package com.attendance.tracker.feature.schedule.model

/**
 * UI-layer representation of schedule timing configurations.
 */
data class ScheduleUiModel(
    val id: Long,
    val subjectId: Long,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String,
    val room: String? = null
)
