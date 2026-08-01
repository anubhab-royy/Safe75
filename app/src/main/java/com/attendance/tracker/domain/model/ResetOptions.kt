package com.attendance.tracker.domain.model

/**
 * Options controlling what is preserved and what is deleted during a semester reset.
 */
data class ResetOptions(
    /** When true, existing subjects are kept in the database. */
    val keepSubjects: Boolean = true,
    /** When true, existing schedules/timetable entries are kept. */
    val keepSchedules: Boolean = true,
    /** When true, user settings (theme, notifications) are preserved. */
    val keepSettings: Boolean = true,
    /** When true, all attendance records are deleted. */
    val deleteAttendance: Boolean = true
)
