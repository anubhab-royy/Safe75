package com.attendance.tracker.domain.model

/**
 * Options controlling which data categories are restored during a backup import
 * or archive restoration.
 *
 * By default all categories are enabled.
 */
data class RestoreOptions(
    val restoreSubjects: Boolean = true,
    val restoreSchedules: Boolean = true,
    val restoreSemesterVersions: Boolean = true,
    val restoreAttendance: Boolean = true,
    val restoreSettings: Boolean = true
)
