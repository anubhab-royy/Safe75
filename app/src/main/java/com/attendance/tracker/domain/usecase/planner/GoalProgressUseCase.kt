package com.attendance.tracker.domain.usecase.planner

import com.attendance.tracker.domain.model.AttendanceStatistics
import javax.inject.Inject

/**
 * Result data class for overall progress towards target attendance percentages.
 */
data class GoalProgressResult(
    val currentPercentage: Double,
    val requiredPercentage: Int,
    val personalGoalPercentage: Int,
    val remainingSafeClasses: Int,
    val classesNeededToReachGoal: Int,
    val status: String // "SAFE", "WARNING", "CRITICAL"
)

/**
 * Domain Use Case to calculate goal completion status and safety zones.
 */
class GoalProgressUseCase @Inject constructor() {

    operator fun invoke(
        stats: AttendanceStatistics,
        requiredPct: Int,
        personalGoalPct: Int
    ): GoalProgressResult {
        val status = when {
            stats.attendancePercentage < requiredPct -> "CRITICAL"
            stats.remainingSafeClasses == 0 -> "WARNING"
            else -> "SAFE"
        }
        return GoalProgressResult(
            currentPercentage = stats.attendancePercentage,
            requiredPercentage = requiredPct,
            personalGoalPercentage = personalGoalPct,
            remainingSafeClasses = stats.remainingSafeClasses,
            classesNeededToReachGoal = stats.classesNeededToReachGoal,
            status = status
        )
    }
}
