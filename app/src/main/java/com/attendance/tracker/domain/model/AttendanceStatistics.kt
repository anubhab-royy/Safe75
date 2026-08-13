package com.attendance.tracker.domain.model

/**
 * Domain model representing calculated attendance statistics.
 */
data class AttendanceStatistics(
    val presentCount: Int,
    val absentCount: Int,
    val cancelledCount: Int,
    val medicalLeaveCount: Int,
    val totalClasses: Int, // present + absent + medical leave (cancelled classes are excluded)
    val attendancePercentage: Double, // present / (present + absent + medical leave) * 100
    val withMedicalPercentage: Double, // (present + medical leave) / (present + absent + medical leave) * 100
    val remainingSafeClasses: Int, // absences allowed consecutively before dropping below required %
    val classesNeededToReachGoal: Int // presences needed consecutively to raise percentage to goal %
)
