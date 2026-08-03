package com.attendance.tracker.domain.usecase.attendance

import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.model.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DetectMissingAttendanceUseCaseTest {

    private val useCase = DetectMissingAttendanceUseCase()

    private fun schedule(
        id: Long,
        subjectId: Long = 1L,
        day: WeekDay,
        start: LocalTime = LocalTime.of(9, 0)
    ) = Schedule(
        id = id,
        subjectId = subjectId,
        dayOfWeek = day,
        startTime = start,
        endTime = LocalTime.of(10, 30),
        versionId = 1L
    )

    @Test
    fun `detect returns nothing when no schedules`() {
        val from = LocalDate.of(2026, 1, 5)
        val to = LocalDate.of(2026, 1, 9)
        val result = useCase(schedules = emptyList(), attendance = emptyList(), from = from, to = to)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `detect returns nothing when range is inverted`() {
        val from = LocalDate.of(2026, 1, 9)
        val to = LocalDate.of(2026, 1, 5)
        val result = useCase(
            schedules = listOf(schedule(id = 1, day = WeekDay.Monday)),
            attendance = emptyList(),
            from = from,
            to = to
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun `detect lists every weekly occurrence without attendance`() {
        // 2026-01-05 is a Monday, 2026-01-09 is a Friday.
        val from = LocalDate.of(2026, 1, 5)
        val to = LocalDate.of(2026, 1, 16)
        val result = useCase(
            schedules = listOf(schedule(id = 1, day = WeekDay.Monday)),
            attendance = emptyList(),
            from = from,
            to = to
        )

        // Mondays in that range: 01-05, 01-12
        assertEquals(listOf(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 12)), result.map { it.date })
        assertTrue(result.all { it.scheduleId == 1L })
        assertTrue(result.all { it.dayOfWeek == WeekDay.Monday })
    }

    @Test
    fun `detect skips dates that already have attendance`() {
        val from = LocalDate.of(2026, 1, 5)
        val to = LocalDate.of(2026, 1, 16)
        val attendance = listOf(
            Attendance(
                subjectId = 1L,
                scheduleId = 1L,
                date = LocalDate.of(2026, 1, 12),
                status = AttendanceStatus.PRESENT
            )
        )
        val result = useCase(
            schedules = listOf(schedule(id = 1, day = WeekDay.Monday)),
            attendance = attendance,
            from = from,
            to = to
        )

        assertEquals(listOf(LocalDate.of(2026, 1, 5)), result.map { it.date })
    }

    @Test
    fun `detect covers multiple schedules and sorts by date then start`() {
        val from = LocalDate.of(2026, 1, 5)
        val to = LocalDate.of(2026, 1, 6)
        val result = useCase(
            schedules = listOf(
                schedule(id = 1, day = WeekDay.Tuesday, start = LocalTime.of(14, 0)),
                schedule(id = 2, day = WeekDay.Tuesday, start = LocalTime.of(9, 0))
            ),
            attendance = emptyList(),
            from = from,
            to = to
        )

        assertEquals(2, result.size)
        // Same date, earlier start first
        assertEquals(listOf(LocalTime.of(9, 0), LocalTime.of(14, 0)), result.map { it.startTime })
    }
}
