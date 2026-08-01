package com.attendance.tracker.core.model

/**
 * Data class representing attendance criteria limits and personal goals.
 *
 * @property requiredPercentage The minimum percentage required by the institution (e.g. 75.0).
 * @property personalGoal The user's personal target goal for attendance (e.g. 85.0).
 */
data class AttendanceTarget(
    val requiredPercentage: Double,
    val personalGoal: Double
)
