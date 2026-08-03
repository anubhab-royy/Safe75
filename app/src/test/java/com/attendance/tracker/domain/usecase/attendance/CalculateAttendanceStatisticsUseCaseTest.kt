package com.attendance.tracker.domain.usecase.attendance

import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.domain.model.Attendance
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests verifying math calculations inside [CalculateAttendanceStatisticsUseCase].
 */
class CalculateAttendanceStatisticsUseCaseTest {

    private val calculateUseCase = CalculateAttendanceStatisticsUseCase()

    @Test
    fun testCancellationExemptions_doNotImpactTotal() {
        val records = listOf(
            Attendance(subjectId = 1, scheduleId = 1, date = java.time.LocalDate.now(), status = AttendanceStatus.PRESENT),
            Attendance(subjectId = 1, scheduleId = 1, date = java.time.LocalDate.now(), status = AttendanceStatus.PRESENT),
            Attendance(subjectId = 1, scheduleId = 1, date = java.time.LocalDate.now(), status = AttendanceStatus.ABSENT),
            Attendance(subjectId = 1, scheduleId = 1, date = java.time.LocalDate.now(), status = AttendanceStatus.CANCELLED),
            Attendance(subjectId = 1, scheduleId = 1, date = java.time.LocalDate.now(), status = AttendanceStatus.CANCELLED)
        )

        val stats = calculateUseCase(records, requiredPercentage = 75, goalPercentage = 80)
        assertEquals(3, stats.totalClasses) // 2 Present + 1 Absent
        assertEquals(2, stats.presentCount)
        assertEquals(1, stats.absentCount)
        assertEquals(2, stats.cancelledCount)
        assertEquals(66.66, stats.attendancePercentage, 0.1)
    }

    @Test
    fun testRemainingSafeAbsences() {
        val records = listOf(
            Attendance(subjectId = 1, scheduleId = 1, date = java.time.LocalDate.now(), status = AttendanceStatus.PRESENT),
            Attendance(subjectId = 1, scheduleId = 1, date = java.time.LocalDate.now(), status = AttendanceStatus.PRESENT),
            Attendance(subjectId = 1, scheduleId = 1, date = java.time.LocalDate.now(), status = AttendanceStatus.PRESENT),
            Attendance(subjectId = 1, scheduleId = 1, date = java.time.LocalDate.now(), status = AttendanceStatus.PRESENT)
        )

        // 4 Present, 0 Absent. R = 75%.
        // (4) / (4 + x) >= 0.75 => 4 >= 3 + 0.75*x => 1 >= 0.75*x => x <= 1.33 => 1 safe miss
        val stats = calculateUseCase(records, requiredPercentage = 75, goalPercentage = 80)
        assertEquals(1, stats.remainingSafeClasses)
    }

    @Test
    fun testClassesNeededToReachGoal() {
        val records = listOf(
            Attendance(subjectId = 1, scheduleId = 1, date = java.time.LocalDate.now(), status = AttendanceStatus.PRESENT),
            Attendance(subjectId = 1, scheduleId = 1, date = java.time.LocalDate.now(), status = AttendanceStatus.ABSENT)
        )

        // 1 Present, 1 Absent. Goal = 80%.
        // (1 + y) / (2 + y) >= 0.8 => 1 + y >= 1.6 + 0.8*y => 0.2*y >= 0.6 => y >= 3 consecutive classes
        val stats = calculateUseCase(records, requiredPercentage = 75, goalPercentage = 80)
        assertEquals(3, stats.classesNeededToReachGoal)
    }

    @Test
    fun testUnreachableGoalOf100Percent() {
        val records = listOf(
            Attendance(subjectId = 1, scheduleId = 1, date = java.time.LocalDate.now(), status = AttendanceStatus.ABSENT)
        )

        val stats = calculateUseCase(records, requiredPercentage = 75, goalPercentage = 100)
        assertEquals(-1, stats.classesNeededToReachGoal) // Unreachable since absent count is > 0
    }

    @Test
    fun testNoAttendanceRecords_returnsNegativeOnePercentage() {
        val stats = calculateUseCase(emptyList(), requiredPercentage = 75, goalPercentage = 80)
        assertEquals(-1.0, stats.attendancePercentage, 0.0)
        assertEquals(0, stats.totalClasses)
        assertEquals(0, stats.presentCount)
    }
}
