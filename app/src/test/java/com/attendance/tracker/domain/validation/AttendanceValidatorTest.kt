package com.attendance.tracker.domain.validation

import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.domain.model.Attendance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests verifying validator constraints inside [AttendanceValidator].
 */
class AttendanceValidatorTest {

    private val validator = AttendanceValidator()

    @Test
    fun testValidAttendance_returnsValid() {
        val attendance = Attendance(
            subjectId = 1L,
            scheduleId = 2L,
            date = LocalDate.now(),
            status = AttendanceStatus.PRESENT
        )
        val result = validator.validate(attendance, isDuplicate = false)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun testFutureDate_returnsInvalid() {
        val attendance = Attendance(
            subjectId = 1L,
            scheduleId = 2L,
            date = LocalDate.now().plusDays(1),
            status = AttendanceStatus.PRESENT
        )
        val result = validator.validate(attendance, isDuplicate = false)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals("Attendance date cannot be in the future", (result as ValidationResult.Invalid).reason)
    }

    @Test
    fun testMissingSubject_returnsInvalid() {
        val attendance = Attendance(
            subjectId = 0L,
            scheduleId = 2L,
            date = LocalDate.now(),
            status = AttendanceStatus.PRESENT
        )
        val result = validator.validate(attendance, isDuplicate = false)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals("Subject must exist", (result as ValidationResult.Invalid).reason)
    }

    @Test
    fun testDuplicateEntry_returnsInvalid() {
        val attendance = Attendance(
            subjectId = 1L,
            scheduleId = 2L,
            date = LocalDate.now(),
            status = AttendanceStatus.PRESENT
        )
        val result = validator.validate(attendance, isDuplicate = true)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals("Attendance has already been marked for this class slot on this date", (result as ValidationResult.Invalid).reason)
    }
}
