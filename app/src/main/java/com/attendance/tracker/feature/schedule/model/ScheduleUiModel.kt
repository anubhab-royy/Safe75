package com.attendance.tracker.feature.schedule.model

/**
 * UI-layer representation of schedule timing configurations for display.
 */
data class ScheduleUiModel(
    val id: Long,
    val subjectId: Long,
    val subjectName: String,
    val subjectColor: Int,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String,
    val room: String? = null,
    val faculty: String? = null
)
