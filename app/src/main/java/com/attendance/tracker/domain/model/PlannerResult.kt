package com.attendance.tracker.domain.model

/**
 * Domain model representing stats of a subject affected by leave days.
 */
data class AffectedSubject(
    val subjectId: Long,
    val subjectName: String,
    val subjectColor: Int,
    val currentPercentage: Double,
    val projectedPercentage: Double,
    val missedClassesCount: Int,
    val isProjectedSafe: Boolean
)

/**
 * Domain model containing leave planner projections and safety recommendations.
 */
data class PlannerResult(
    val affectedSubjects: List<AffectedSubject>,
    val isOverallSafe: Boolean,
    val currentOverallPercentage: Double,
    val projectedOverallPercentage: Double
)
