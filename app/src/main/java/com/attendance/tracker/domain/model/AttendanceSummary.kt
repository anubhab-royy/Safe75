package com.attendance.tracker.domain.model

/**
 * Domain model summarizing a subject's details and active attendance stats.
 */
data class AttendanceSummary(
    val subjectId: Long,
    val subjectName: String,
    val subjectColor: Int,
    val statistics: AttendanceStatistics,
    val requiredAttendancePercentage: Int,
    val personalAttendanceGoal: Int
)
