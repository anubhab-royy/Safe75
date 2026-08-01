package com.attendance.tracker.feature.dashboard.model

/**
 * UI-layer model summarizing overall student attendance statistics on the Dashboard.
 */
data class DashboardUiModel(
    val overallPercentage: Double,
    val totalSubjects: Int,
    val targetStatusMessage: String,
    val isGoalMet: Boolean
)
