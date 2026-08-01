package com.attendance.tracker.domain.usecase.planner

import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.model.AttendanceStatistics
import com.attendance.tracker.domain.usecase.attendance.CalculateAttendanceStatisticsUseCase
import javax.inject.Inject

/**
 * Domain Use Case to compute bunking thresholds (safe absences and goal classes needed)
 * at the subject and overall levels.
 */
class SafeBunkCalculatorUseCase @Inject constructor(
    private val calculateStatisticsUseCase: CalculateAttendanceStatisticsUseCase
) {
    operator fun invoke(
        records: List<Attendance>,
        requiredPercentage: Int,
        goalPercentage: Int
    ): AttendanceStatistics {
        return calculateStatisticsUseCase(records, requiredPercentage, goalPercentage)
    }
}
