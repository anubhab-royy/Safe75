package com.attendance.tracker.domain.usecase.planner

import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.usecase.attendance.CalculateAttendanceStatisticsUseCase
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SafeBunkCalculatorUseCaseTest {

    private val calculateStatsUseCase = CalculateAttendanceStatisticsUseCase()
    private val calculator = SafeBunkCalculatorUseCase(calculateStatsUseCase)

    @Test
    fun testCalculatorDelegation() {
        val records = listOf(
            Attendance(subjectId = 1, scheduleId = 10, date = LocalDate.now(), status = AttendanceStatus.PRESENT),
            Attendance(subjectId = 1, scheduleId = 10, date = LocalDate.now(), status = AttendanceStatus.PRESENT),
            Attendance(subjectId = 1, scheduleId = 10, date = LocalDate.now(), status = AttendanceStatus.ABSENT)
        )

        val stats = calculator(records, requiredPercentage = 75, goalPercentage = 80)
        assertEquals(3, stats.totalClasses)
        assertEquals(2, stats.presentCount)
        assertEquals(1, stats.absentCount)
        assertEquals(66.6, stats.attendancePercentage, 0.1)
    }
}
