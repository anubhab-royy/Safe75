package com.attendance.tracker.domain.usecase.attendance

import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.domain.model.Attendance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests verifying math calculations inside [CalculateAttendanceStatisticsUseCase].
 */
class CalculateAttendanceStatisticsUseCaseTest {

    private val calculateUseCase = CalculateAttendanceStatisticsUseCase()

    @Test
    fun testCancellationExemptions_doNotImpactTotal() {
        val records = listOf(
            Attendance(subjectId = 1, scheduleId = 1, date = LocalDate.now(), status = AttendanceStatus.PRESENT),
            Attendance(subjectId = 1, scheduleId = 1, date = LocalDate.now(), status = AttendanceStatus.PRESENT),
            Attendance(subjectId = 1, scheduleId = 1, date = LocalDate.now(), status = AttendanceStatus.ABSENT),
            Attendance(subjectId = 1, scheduleId = 1, date = LocalDate.now(), status = AttendanceStatus.CANCELLED),
            Attendance(subjectId = 1, scheduleId = 1, date = LocalDate.now(), status = AttendanceStatus.CANCELLED)
        )

        val stats = calculateUseCase(records, requiredPercentage = 75, goalPercentage = 80)
        assertEquals(3, stats.totalClasses) // 2 Present + 1 Absent
        assertEquals(2, stats.presentCount)
        assertEquals(1, stats.absentCount)
        assertEquals(2, stats.cancelledCount)
        assertEquals(0, stats.medicalLeaveCount)
        assertEquals(66.66, stats.attendancePercentage, 0.1)
        assertEquals(66.66, stats.withMedicalPercentage, 0.1)
    }

    @Test
    fun testMedicalLeave_includedInTotalClassesAndDenominator() {
        val records = listOf(
            Attendance(subjectId = 1, scheduleId = 1, date = LocalDate.now(), status = AttendanceStatus.PRESENT),
            Attendance(subjectId = 1, scheduleId = 1, date = LocalDate.now(), status = AttendanceStatus.ABSENT),
            Attendance(subjectId = 1, scheduleId = 1, date = LocalDate.now(), status = AttendanceStatus.MEDICAL_LEAVE)
        )

        val stats = calculateUseCase(records, requiredPercentage = 75, goalPercentage = 80)

        assertEquals(1, stats.medicalLeaveCount)
        assertEquals(3, stats.totalClasses) // Present(1) + Absent(1) + Medical(1) = 3
        assertEquals(33.33, stats.attendancePercentage, 0.01) // 1 / 3
        assertEquals(66.67, stats.withMedicalPercentage, 0.01) // (1 + 1) / 3
    }

    @Test
    fun testOnlyMedicalLeave_isNotTreatedAsEmpty() {
        val records = listOf(
            Attendance(subjectId = 1, scheduleId = 1, date = LocalDate.now(), status = AttendanceStatus.MEDICAL_LEAVE),
            Attendance(subjectId = 1, scheduleId = 1, date = LocalDate.now(), status = AttendanceStatus.MEDICAL_LEAVE)
        )

        val stats = calculateUseCase(records, requiredPercentage = 75, goalPercentage = 80)

        assertEquals(2, stats.medicalLeaveCount)
        assertEquals(2, stats.totalClasses) // 2 Medical Leave classes exist
        assertEquals(0.0, stats.attendancePercentage, 0.0) // Present(0) / 2 = 0%
        assertEquals(100.0, stats.withMedicalPercentage, 0.0) // (0 + 2) / 2 = 100%
        assertEquals(0, stats.remainingSafeClasses)
    }

    @Test
    fun testMedicalLeaveAndCancellation_cancelledExcludedMedicalIncluded() {
        val records = listOf(
            Attendance(subjectId = 1, scheduleId = 1, date = LocalDate.now(), status = AttendanceStatus.PRESENT),
            Attendance(subjectId = 1, scheduleId = 1, date = LocalDate.now(), status = AttendanceStatus.ABSENT),
            Attendance(subjectId = 1, scheduleId = 1, date = LocalDate.now(), status = AttendanceStatus.MEDICAL_LEAVE),
            Attendance(subjectId = 1, scheduleId = 1, date = LocalDate.now(), status = AttendanceStatus.CANCELLED)
        )

        val stats = calculateUseCase(records, requiredPercentage = 75, goalPercentage = 80)

        assertEquals(1, stats.cancelledCount)
        assertEquals(1, stats.medicalLeaveCount)
        assertEquals(3, stats.totalClasses) // Present(1) + Absent(1) + Medical(1)
        assertEquals(33.33, stats.attendancePercentage, 0.01)
        assertEquals(66.67, stats.withMedicalPercentage, 0.01)
    }

    // Authoritative Case 1: P=0, A=1, ML=2, C=1 -> total=3, normal=0.0%, withMedical=66.7%
    @Test
    fun testAuthoritativeCase1() {
        val stats = calculateUseCase(records(present = 0, absent = 1, medicalLeave = 2, cancelled = 1), 75, 80)
        assertEquals(3, stats.totalClasses)
        assertEquals(0.0, stats.attendancePercentage, 0.01)
        assertEquals(66.67, stats.withMedicalPercentage, 0.01)
    }

    // Authoritative Case 2: P=0, A=1, ML=3, C=3 -> total=4, normal=0.0%, withMedical=75.0%
    @Test
    fun testAuthoritativeCase2() {
        val stats = calculateUseCase(records(present = 0, absent = 1, medicalLeave = 3, cancelled = 3), 75, 80)
        assertEquals(4, stats.totalClasses)
        assertEquals(0.0, stats.attendancePercentage, 0.01)
        assertEquals(75.0, stats.withMedicalPercentage, 0.01)
    }

    // Authoritative Case 3: P=5, A=2, ML=3, C=4 -> total=10, normal=50.0%, withMedical=80.0%
    @Test
    fun testAuthoritativeCase3() {
        val stats = calculateUseCase(records(present = 5, absent = 2, medicalLeave = 3, cancelled = 4), 75, 80)
        assertEquals(10, stats.totalClasses)
        assertEquals(50.0, stats.attendancePercentage, 0.01)
        assertEquals(80.0, stats.withMedicalPercentage, 0.01)
    }

    // Authoritative Case 4: P=10, A=0, ML=0, C=5 -> total=10, normal=100.0%, withMedical=100.0%
    @Test
    fun testAuthoritativeCase4() {
        val stats = calculateUseCase(records(present = 10, absent = 0, medicalLeave = 0, cancelled = 5), 75, 80)
        assertEquals(10, stats.totalClasses)
        assertEquals(100.0, stats.attendancePercentage, 0.01)
        assertEquals(100.0, stats.withMedicalPercentage, 0.01)
    }

    // Authoritative Case 5: P=0, A=0, ML=5, C=2 -> total=5, normal=0.0%, withMedical=100.0%
    @Test
    fun testAuthoritativeCase5() {
        val stats = calculateUseCase(records(present = 0, absent = 0, medicalLeave = 5, cancelled = 2), 75, 80)
        assertEquals(5, stats.totalClasses)
        assertEquals(0.0, stats.attendancePercentage, 0.01)
        assertEquals(100.0, stats.withMedicalPercentage, 0.01)
    }

    // ML5.2 Test1 Regression: P=1, A=1, ML=1, Goal=50% -> normal=33.3%, classesNeeded=1
    @Test
    fun testMl52_Test1_Regression_P1_A1_ML1_Goal50() {
        val stats = calculateUseCase(records(present = 1, absent = 1, medicalLeave = 1), requiredPercentage = 50, goalPercentage = 50)
        assertEquals(3, stats.totalClasses)
        assertEquals(33.33, stats.attendancePercentage, 0.01)
        assertEquals(1, stats.classesNeededToReachGoal) // (1 + 1) / (3 + 1) = 2/4 = 50%
    }

    // ML5.2 Test2 Regression: P=0, A=1, ML=2, Goal=75% -> normal=0.0%, classesNeeded=9
    @Test
    fun testMl52_Test2_Regression_P0_A1_ML2_Goal75() {
        val stats = calculateUseCase(records(present = 0, absent = 1, medicalLeave = 2), requiredPercentage = 75, goalPercentage = 75)
        assertEquals(3, stats.totalClasses)
        assertEquals(0.0, stats.attendancePercentage, 0.01)
        assertEquals(9, stats.classesNeededToReachGoal) // (0 + 9) / (3 + 9) = 9/12 = 75%
    }

    // ML5.2 Dashboard Overall Regression: P=1, A=2, ML=3, C=1, Goal=50% -> normal=16.7%, classesNeeded=4
    @Test
    fun testMl52_DashboardOverall_Regression_P1_A2_ML3_C1_Goal50() {
        val stats = calculateUseCase(records(present = 1, absent = 2, medicalLeave = 3, cancelled = 1), requiredPercentage = 50, goalPercentage = 50)
        assertEquals(6, stats.totalClasses)
        assertEquals(16.67, stats.attendancePercentage, 0.01)
        assertEquals(4, stats.classesNeededToReachGoal) // (1 + 4) / (6 + 4) = 5/10 = 50%
    }

    @Test
    fun testGoalOf100Percent_withAbsencesOrMedical_isUnreachable() {
        val withAbsent = calculateUseCase(records(present = 5, absent = 1), 75, 100)
        assertEquals(-1, withAbsent.classesNeededToReachGoal)

        val withMedical = calculateUseCase(records(present = 5, medicalLeave = 1), 75, 100)
        assertEquals(-1, withMedical.classesNeededToReachGoal)

        val perfect = calculateUseCase(records(present = 5), 75, 100)
        assertEquals(0, perfect.classesNeededToReachGoal)
    }

    @Test
    fun testPercentageNeverExceeds100() {
        val stats = calculateUseCase(records(present = 5, absent = 0, medicalLeave = 10, cancelled = 2), 75, 80)
        assertTrue(stats.attendancePercentage <= 100.0)
        assertTrue(stats.withMedicalPercentage <= 100.0)
    }

    @Test
    fun testNoAttendanceRecords_returnsNegativeOnePercentage() {
        val stats = calculateUseCase(emptyList(), requiredPercentage = 75, goalPercentage = 80)
        assertEquals(-1.0, stats.attendancePercentage, 0.0)
        assertEquals(-1.0, stats.withMedicalPercentage, 0.0)
        assertEquals(0, stats.totalClasses)
        assertEquals(0, stats.presentCount)
    }

    private fun records(
        present: Int = 0,
        absent: Int = 0,
        cancelled: Int = 0,
        medicalLeave: Int = 0
    ): List<Attendance> {
        val statuses = buildList {
            repeat(present) { add(AttendanceStatus.PRESENT) }
            repeat(absent) { add(AttendanceStatus.ABSENT) }
            repeat(cancelled) { add(AttendanceStatus.CANCELLED) }
            repeat(medicalLeave) { add(AttendanceStatus.MEDICAL_LEAVE) }
        }
        return statuses.mapIndexed { index, status ->
            Attendance(
                subjectId = 1,
                scheduleId = 1,
                date = LocalDate.now().minusDays(index.toLong()),
                status = status
            )
        }
    }
}
