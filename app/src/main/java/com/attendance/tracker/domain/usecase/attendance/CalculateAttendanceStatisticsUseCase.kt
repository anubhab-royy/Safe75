package com.attendance.tracker.domain.usecase.attendance

import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.model.AttendanceStatistics
import javax.inject.Inject
import kotlin.math.ceil

/**
 * Domain Use Case to calculate attendance statistics.
 * Excludes CANCELLED classes from non-cancelled totals while including MEDICAL_LEAVE in display total.
 * Predictive metrics (remaining safe classes and classes needed to reach goal) predict future attendance
 * mathematically consistent with displayed normal attendance (present / nonCancelledTotal).
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
        val medicalLeave = records.count { it.status == AttendanceStatus.MEDICAL_LEAVE }

        val displayTotal = present + absent + medicalLeave

        val percentage = if (displayTotal > 0) {
            (present.toDouble() / displayTotal.toDouble()) * 100.0
        } else {
            -1.0
        }

        val withMedicalPercentage = if (displayTotal > 0) {
            ((present + medicalLeave).toDouble() / displayTotal.toDouble()) * 100.0
        } else {
            -1.0
        }

        val r = requiredPercentage.toDouble()
        val remainingSafe = if (displayTotal > 0 && percentage >= r) {
            val limit = (100.0 * present - r * displayTotal) / r
            limit.toInt()
        } else {
            0
        }

        val g = goalPercentage.toDouble()
        val needed = if (g >= 100.0) {
            if (absent > 0 || medicalLeave > 0) {
                -1 // Goal of 100% is mathematically unreachable if any non-present classes exist
            } else {
                0
            }
        } else if (displayTotal > 0 && percentage < g) {
            val num = g * displayTotal - 100.0 * present
            val den = 100.0 - g
            ceil(num / den).toInt().coerceAtLeast(0)
        } else {
            0
        }

        return AttendanceStatistics(
            presentCount = present,
            absentCount = absent,
            cancelledCount = cancelled,
            medicalLeaveCount = medicalLeave,
            totalClasses = displayTotal,
            attendancePercentage = percentage,
            withMedicalPercentage = withMedicalPercentage,
            remainingSafeClasses = remainingSafe,
            classesNeededToReachGoal = needed
        )
    }
}
