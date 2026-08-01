package com.attendance.tracker.domain.validation

import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.domain.model.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

/**
 * Unit tests verifying business rule constraints in [ScheduleValidator].
 */
class ScheduleValidatorTest {

    private val validator = ScheduleValidator()

    @Test
    fun testValidSchedule_returnsValid() {
        val schedule = Schedule(
            subjectId = 1L,
            dayOfWeek = WeekDay.Monday,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0),
            room = "302",
            versionId = 2L
        )
        val result = validator.validate(schedule, emptyList())
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun testEndTimeBeforeStartTime_returnsInvalid() {
        val schedule = Schedule(
            subjectId = 1L,
            dayOfWeek = WeekDay.Monday,
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(9, 0),
            versionId = 2L
        )
        val result = validator.validate(schedule, emptyList())
        assertTrue(result is ValidationResult.Invalid)
        assertEquals("Start time must be strictly before end time", (result as ValidationResult.Invalid).reason)
    }

    @Test
    fun testTimeOverlap_returnsInvalid() {
        val existing = listOf(
            Schedule(
                id = 10L,
                subjectId = 2L,
                dayOfWeek = WeekDay.Monday,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(10, 30),
                versionId = 2L
            )
        )

        // Overlapping slot: 10:00 to 11:00 overlaps with 09:00 to 10:30
        val schedule = Schedule(
            id = 11L,
            subjectId = 3L,
            dayOfWeek = WeekDay.Monday,
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            versionId = 2L
        )

        val result = validator.validate(schedule, existing)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals("Schedule overlaps with an existing class", (result as ValidationResult.Invalid).reason)
    }

    @Test
    fun testMissingSubject_returnsInvalid() {
        val schedule = Schedule(
            subjectId = 0L,
            dayOfWeek = WeekDay.Monday,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0),
            versionId = 2L
        )
        val result = validator.validate(schedule, emptyList())
        assertTrue(result is ValidationResult.Invalid)
        assertEquals("Subject must exist", (result as ValidationResult.Invalid).reason)
    }

    @Test
    fun testMissingVersion_returnsInvalid() {
        val schedule = Schedule(
            subjectId = 1L,
            dayOfWeek = WeekDay.Monday,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0),
            versionId = 0L
        )
        val result = validator.validate(schedule, emptyList())
        assertTrue(result is ValidationResult.Invalid)
        assertEquals("Version required", (result as ValidationResult.Invalid).reason)
    }

    @Test
    fun testRoomTooLong_returnsInvalid() {
        val longRoom = "R".repeat(101)
        val schedule = Schedule(
            subjectId = 1L,
            dayOfWeek = WeekDay.Monday,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0),
            room = longRoom,
            versionId = 2L
        )
        val result = validator.validate(schedule, emptyList())
        assertTrue(result is ValidationResult.Invalid)
        assertEquals("Room location name cannot exceed 100 characters", (result as ValidationResult.Invalid).reason)
    }
}
