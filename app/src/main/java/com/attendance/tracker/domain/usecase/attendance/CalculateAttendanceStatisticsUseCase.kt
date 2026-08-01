package com.attendance.tracker.domain.usecase.attendance

import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.model.AttendanceStatistics
import javax.inject.Inject
import kotlin.math.ceil

/**
 * Domain Use Case to calculate attendance statistics.
 * Excludes CANCELLED classes from total counts and implements attendance percentages,
 * safe miss tolerances, and consecutive classes needed to raise attendance to a goal.
 */
class CalculateAttendanceStatisticsUseCase @Inject constructor() {

    operator fun invoke(
        records: List<Attendance>,
        requiredPercentage: Int,
        goalPercentage: Int
    ): AttendanceStatistics {
        val present = records.count { it.status == AttendanceStatus.PRESENT }
        val absent = records.count { it.status == AttendanceStatus.ABSENT }
        val cancelled = records.count { it.status == AttendanceStatus.CANCELLED }
        val total = present + absent

        val percentage = if (total > 0) {
            (present.toDouble() / total.toDouble()) * 100.0
        } else {
            100.0
        }

        val r = requiredPercentage.toDouble()
        val remainingSafe = if (total > 0 && percentage >= r) {
            val limit = (100.0 * present - r * total) / r
            limit.toInt()
        } else {
            0
        }

        val g = goalPercentage.toDouble()
        val needed = if (g >= 100.0) {
            if (absent > 0) {
                -1 // Goal is mathematically unreachable
            } else {
                0
            }
        } else if (percentage < g) {
            val num = g * total - 100.0 * present
            val den = 100.0 - g
            ceil(num / den).toInt().coerceAtLeast(0)
        } else {
            0
        }

        return AttendanceStatistics(
            presentCount = present,
            absentCount = absent,
            cancelledCount = cancelled,
            totalClasses = total,
            attendancePercentage = percentage,
            remainingSafeClasses = remainingSafe,
            classesNeededToReachGoal = needed
        )
    }
}
