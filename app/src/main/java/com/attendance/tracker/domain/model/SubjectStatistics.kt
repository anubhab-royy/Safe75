package com.attendance.tracker.domain.model

/**
 * Domain model representing stats and goal milestones on a per-subject basis.
 */
data class SubjectStatistics(
    val subjectId: Long,
    val subjectName: String,
    val subjectColor: Int,
    val presentCount: Int,
    val totalClasses: Int,
    val percentage: Double,
    val withMedicalPercentage: Double,
    val requiredPercentage: Int,
    val personalGoalPercentage: Int,
    val safetyStatus: String, // "SAFE", "WARNING", "CRITICAL"
    val safeMissCount: Int,
    val classesNeeded: Int
)
