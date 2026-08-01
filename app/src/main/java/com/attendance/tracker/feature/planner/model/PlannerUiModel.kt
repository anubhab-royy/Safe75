package com.attendance.tracker.feature.planner.model

/**
 * UI-layer representation of study plan tasks.
 */
data class PlannerUiModel(
    val id: Long,
    val taskTitle: String,
    val dueDate: String,
    val isCompleted: Boolean
)
