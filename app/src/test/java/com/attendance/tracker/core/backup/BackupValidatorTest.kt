package com.attendance.tracker.core.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [BackupValidator] covering schema, duplicates, and referential integrity.
 */
class BackupValidatorTest {

    private val validator = BackupValidator()

    private val now = 1_700_000_000_000L

    private fun validBackup(): BackupData = BackupData(
        metadata = BackupMetadata(backupVersion = 1, appVersion = "1.0", schemaVersion = 1, createdAt = now),
        subjects = listOf(
            BackupSubjectDto(1L, "Math", createdAt = now, updatedAt = now),
            BackupSubjectDto(2L, "Physics", createdAt = now, updatedAt = now)
        ),
        schedules = listOf(
            BackupScheduleDto(10L, 1L, "Monday", "09:00", "10:00", versionId = 5L, createdAt = now, updatedAt = now),
            BackupScheduleDto(11L, 2L, "Tuesday", "10:00", "11:00", versionId = 5L, createdAt = now, updatedAt = now)
        ),
        semesterVersions = listOf(
            BackupSemesterDto(5L, "Fall 2026", isActive = true, createdAt = now)
        ),
        attendanceRecords = listOf(
            BackupAttendanceDto(100L, 1L, 10L, "2026-08-02", "PRESENT", createdAt = now, updatedAt = now)
        )
    )

    @Test
    fun testValidBackup_passesAllChecks() {
        val result = validator.validate(validBackup())
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun testUnsupportedSchema_rejectedEarly() {
        val data = validBackup().copy(
            metadata = BackupMetadata(backupVersion = 1, appVersion = "1.0", schemaVersion = 99, createdAt = now)
        )
        val result = validator.validate(data)
        assertTrue(result is ValidationResult.Invalid)
        val errors = (result as ValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("schema version 99") })
    }

    @Test
    fun testBlankAppVersion_rejected() {
        val data = validBackup().copy(
            metadata = BackupMetadata(backupVersion = 1, appVersion = "  ", schemaVersion = 1, createdAt = now)
        )
        val result = validator.validate(data)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun testInvalidCreatedAt_rejected() {
        val data = validBackup().copy(
            metadata = BackupMetadata(backupVersion = 1, appVersion = "1.0", schemaVersion = 1, createdAt = 0L)
        )
        val result = validator.validate(data)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun testDuplicateSubjectIds_rejected() {
        val data = validBackup().copy(
            subjects = listOf(
                BackupSubjectDto(1L, "Math", createdAt = now, updatedAt = now),
                BackupSubjectDto(1L, "Duplicate", createdAt = now, updatedAt = now)
            )
        )
        val result = validator.validate(data)
        assertTrue(result is ValidationResult.Invalid)
        val errors = (result as ValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("duplicate subject IDs") })
    }

    @Test
    fun testDuplicateScheduleIds_rejected() {
        val data = validBackup().copy(
            schedules = listOf(
                BackupScheduleDto(10L, 1L, "Monday", "09:00", "10:00", versionId = 5L, createdAt = now, updatedAt = now),
                BackupScheduleDto(10L, 2L, "Tuesday", "10:00", "11:00", versionId = 5L, createdAt = now, updatedAt = now)
            )
        )
        val result = validator.validate(data)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun testScheduleMissingSubject_rejected() {
        val data = validBackup().copy(
            schedules = listOf(
                BackupScheduleDto(10L, 99L, "Monday", "09:00", "10:00", versionId = 5L, createdAt = now, updatedAt = now)
            )
        )
        val result = validator.validate(data)
        assertTrue(result is ValidationResult.Invalid)
        val errors = (result as ValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("missing subjects") })
    }

    @Test
    fun testScheduleMissingSemesterVersion_rejected() {
        val data = validBackup().copy(
            semesterVersions = emptyList()
        )
        val result = validator.validate(data)
        assertTrue(result is ValidationResult.Invalid)
        val errors = (result as ValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("missing semester") })
    }

    @Test
    fun testAttendanceMissingSchedule_rejected() {
        val data = validBackup().copy(
            attendanceRecords = listOf(
                BackupAttendanceDto(100L, 1L, 404L, "2026-08-02", "PRESENT", createdAt = now, updatedAt = now)
            )
        )
        val result = validator.validate(data)
        assertTrue(result is ValidationResult.Invalid)
        val errors = (result as ValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("missing schedules") })
    }

    @Test
    fun testAttendanceMissingSubject_rejected() {
        val data = validBackup().copy(
            attendanceRecords = listOf(
                BackupAttendanceDto(100L, 404L, 10L, "2026-08-02", "PRESENT", createdAt = now, updatedAt = now)
            )
        )
        val result = validator.validate(data)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun testInvalidAttendanceStatus_rejected() {
        val data = validBackup().copy(
            attendanceRecords = listOf(
                BackupAttendanceDto(100L, 1L, 10L, "2026-08-02", "UNKNOWN", createdAt = now, updatedAt = now)
            )
        )
        val result = validator.validate(data)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun testInvalidDayOfWeek_rejected() {
        val data = validBackup().copy(
            schedules = listOf(
                BackupScheduleDto(10L, 1L, "Funday", "09:00", "10:00", versionId = 5L, createdAt = now, updatedAt = now)
            )
        )
        val result = validator.validate(data)
        assertTrue(result is ValidationResult.Invalid)
        val errors = (result as ValidationResult.Invalid).errors
        assertTrue(errors.any { it.contains("dayOfWeek") })
    }
}
