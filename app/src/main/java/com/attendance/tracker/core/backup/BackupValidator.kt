package com.attendance.tracker.core.backup

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates a [BackupData] payload before it is imported into the database.
 *
 * Checks performed:
 * 1. Schema version compatibility
 * 2. Required metadata fields
 * 3. Referential integrity (attendance → subjects, attendance → schedules, schedules → semesters)
 * 4. Duplicate ID detection
 */
@Singleton
class BackupValidator @Inject constructor() {

    /**
     * Validates a parsed [BackupData].
     *
     * @return [ValidationResult.Valid] if the backup passes all checks, or
     *         [ValidationResult.Invalid] with a list of human-readable error messages.
     */
    fun validate(data: BackupData): ValidationResult {
        val errors = mutableListOf<String>()

        // 1. Schema version check
        if (data.metadata.schemaVersion > CURRENT_SCHEMA_VERSION) {
            errors.add(
                "Unsupported schema version ${data.metadata.schemaVersion}. " +
                "Maximum supported version is $CURRENT_SCHEMA_VERSION. " +
                "Please update the app."
            )
            // Cannot continue safely — return early
            return ValidationResult.Invalid(errors)
        }

        // 2. Required metadata fields
        if (data.metadata.appVersion.isBlank()) {
            errors.add("Backup metadata is missing 'appVersion'.")
        }
        if (data.metadata.createdAt <= 0L) {
            errors.add("Backup metadata has an invalid 'createdAt' timestamp.")
        }

        // 3. Duplicate ID detection
        val subjectIds = data.subjects.map { it.id }
        if (subjectIds.size != subjectIds.toSet().size) {
            errors.add("Backup contains duplicate subject IDs.")
        }

        val scheduleIds = data.schedules.map { it.id }
        if (scheduleIds.size != scheduleIds.toSet().size) {
            errors.add("Backup contains duplicate schedule IDs.")
        }

        val attendanceIds = data.attendanceRecords.map { it.id }
        if (attendanceIds.size != attendanceIds.toSet().size) {
            errors.add("Backup contains duplicate attendance record IDs.")
        }

        val semesterIds = data.semesterVersions.map { it.id }
        if (semesterIds.size != semesterIds.toSet().size) {
            errors.add("Backup contains duplicate semester version IDs.")
        }

        // 4. Referential integrity
        val subjectIdSet = subjectIds.toSet()
        val scheduleIdSet = scheduleIds.toSet()
        val semesterIdSet = semesterIds.toSet()

        // Schedules must reference existing subjects
        val orphanSchedules = data.schedules.filter { it.subjectId !in subjectIdSet }
        if (orphanSchedules.isNotEmpty()) {
            errors.add(
                "${orphanSchedules.size} schedule(s) reference missing subjects: " +
                orphanSchedules.take(3).joinToString { "#${it.id}" }
            )
        }

        // Schedules must reference existing semester versions
        val schedulesWithMissingSemester = data.schedules.filter { it.versionId !in semesterIdSet }
        if (schedulesWithMissingSemester.isNotEmpty()) {
            errors.add(
                "${schedulesWithMissingSemester.size} schedule(s) reference missing semester " +
                "versions: " + schedulesWithMissingSemester.take(3).joinToString { "#${it.id}" }
            )
        }

        // Attendance must reference existing subjects
        val orphanAttendanceBySubject = data.attendanceRecords.filter { it.subjectId !in subjectIdSet }
        if (orphanAttendanceBySubject.isNotEmpty()) {
            errors.add(
                "${orphanAttendanceBySubject.size} attendance record(s) reference missing subjects."
            )
        }

        // Attendance must reference existing schedules
        val orphanAttendanceBySchedule = data.attendanceRecords.filter { it.scheduleId !in scheduleIdSet }
        if (orphanAttendanceBySchedule.isNotEmpty()) {
            errors.add(
                "${orphanAttendanceBySchedule.size} attendance record(s) reference missing schedules."
            )
        }

        // 5. Status value check
        val validStatuses = setOf("PRESENT", "ABSENT", "CANCELLED", "MEDICAL_LEAVE")
        val invalidStatusRecords = data.attendanceRecords.filter { it.status !in validStatuses }
        if (invalidStatusRecords.isNotEmpty()) {
            errors.add(
                "${invalidStatusRecords.size} attendance record(s) have unrecognized status values."
            )
        }

        // 6. Day-of-week check
        val validDays = setOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        val invalidDaySchedules = data.schedules.filter { it.dayOfWeek !in validDays }
        if (invalidDaySchedules.isNotEmpty()) {
            errors.add(
                "${invalidDaySchedules.size} schedule(s) have unrecognized dayOfWeek values."
            )
        }

        return if (errors.isEmpty()) {
            ValidationResult.Valid(data)
        } else {
            ValidationResult.Invalid(errors)
        }
    }
}

/**
 * Result of a [BackupValidator.validate] call.
 */
sealed class ValidationResult {
    /** The backup passed all validation checks. */
    data class Valid(val data: BackupData) : ValidationResult()

    /** The backup failed one or more validation checks. */
    data class Invalid(val errors: List<String>) : ValidationResult()
}
