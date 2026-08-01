package com.attendance.tracker.domain.model

/**
 * Domain model representing calculated attendance statistics.
 */
data class AttendanceStatistics(
    val presentCount: Int,
    val absentCount: Int,
    val cancelledCount: Int,
    val totalClasses: Int, // present + absent (cancelled are excluded)
    val attendancePercentage: Double, // present / (present + absent) * 100
    val remainingSafeClasses: Int, // absences allowed consecutively before dropping below required %
    val classesNeededToReachGoal: Int // presences needed consecutively to raise percentage to goal %
)
