package com.attendance.tracker.domain.model

/**
 * Read-only preview of record counts affected by a semester reset.
 * Shown to the user in the reset wizard before confirmation.
 */
data class ResetPreview(
    /** Number of subjects currently in the database. */
    val subjectCount: Int = 0,
    /** Number of schedules currently in the database. */
    val scheduleCount: Int = 0,
    /** Number of attendance records currently in the database. */
    val attendanceCount: Int = 0,
    /** Number of semester versions currently in the database. */
    val semesterVersionCount: Int = 0
)
