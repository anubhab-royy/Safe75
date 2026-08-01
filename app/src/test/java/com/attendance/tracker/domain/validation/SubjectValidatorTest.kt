package com.attendance.tracker.domain.validation

import com.attendance.tracker.domain.model.Subject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying business rule constraints in [SubjectValidator].
 */
class SubjectValidatorTest {

    private val validator = SubjectValidator()

    @Test
    fun testValidSubject_returnsValid() {
        val subject = Subject(
            name = "Mathematics",
            facultyName = "Dr. John Doe",
            requiredAttendancePercentage = 75,
            personalAttendanceGoal = 85
        )
        val result = validator.validate(subject, emptyList())
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun testEmptyName_returnsInvalid() {
        val subject = Subject(
            name = "   ",
            requiredAttendancePercentage = 75,
            personalAttendanceGoal = 85
        )
        val result = validator.validate(subject, emptyList())
        assertTrue(result is ValidationResult.Invalid)
        assertEquals("Subject name is required", (result as ValidationResult.Invalid).reason)
    }

    @Test
    fun testNameTooLong_returnsInvalid() {
        val longName = "A".repeat(101)
        val subject = Subject(
            name = longName,
            requiredAttendancePercentage = 75,
            personalAttendanceGoal = 85
        )
        val result = validator.validate(subject, emptyList())
        assertTrue(result is ValidationResult.Invalid)
        assertEquals("Subject name cannot exceed 100 characters", (result as ValidationResult.Invalid).reason)
    }

    @Test
    fun testDuplicateName_returnsInvalid() {
        val existing = listOf(Subject(id = 1L, name = "Physics"))
        val subject = Subject(
            id = 2L,
            name = "Physics",
            requiredAttendancePercentage = 75,
            personalAttendanceGoal = 85
        )
        val result = validator.validate(subject, existing)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals("Subject name must be unique", (result as ValidationResult.Invalid).reason)
    }

    @Test
    fun testDuplicateNameSameSubject_returnsValid() {
        val existing = listOf(Subject(id = 1L, name = "Physics"))
        val subject = Subject(
            id = 1L, // Same ID, representing an edit/update check
            name = "Physics",
            requiredAttendancePercentage = 75,
            personalAttendanceGoal = 85
        )
        val result = validator.validate(subject, existing)
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun testRequiredAttendanceRange_returnsInvalid() {
        val subjectUnder = Subject(
            name = "Physics",
            requiredAttendancePercentage = 0,
            personalAttendanceGoal = 85
        )
        val resultUnder = validator.validate(subjectUnder, emptyList())
        assertTrue(resultUnder is ValidationResult.Invalid)

        val subjectOver = Subject(
            name = "Physics",
            requiredAttendancePercentage = 101,
            personalAttendanceGoal = 85
        )
        val resultOver = validator.validate(subjectOver, emptyList())
        assertTrue(resultOver is ValidationResult.Invalid)
    }

    @Test
    fun testGoalLessThanRequired_returnsInvalid() {
        val subject = Subject(
            name = "Physics",
            requiredAttendancePercentage = 75,
            personalAttendanceGoal = 70
        )
        val result = validator.validate(subject, emptyList())
        assertTrue(result is ValidationResult.Invalid)
        assertEquals("Personal goal must be between required attendance and 100%", (result as ValidationResult.Invalid).reason)
    }

    @Test
    fun testFacultyNameTooLong_returnsInvalid() {
        val longFaculty = "T".repeat(101)
        val subject = Subject(
            name = "Physics",
            facultyName = longFaculty,
            requiredAttendancePercentage = 75,
            personalAttendanceGoal = 85
        )
        val result = validator.validate(subject, emptyList())
        assertTrue(result is ValidationResult.Invalid)
        assertEquals("Faculty name cannot exceed 100 characters", (result as ValidationResult.Invalid).reason)
    }
}
