package com.attendance.tracker.domain.model

/**
 * Domain model representing overall metrics and status indicators for the dashboard.
 */
data class DashboardStatistics(
    val overallPercentage: Double,
    val presentCount: Int,
    val absentCount: Int,
    val cancelledCount: Int,
    val totalClasses: Int,
    val safetyStatus: String, // "SAFE", "WARNING", "CRITICAL"
    val safeMissCount: Int,
    val classesNeeded: Int
)
