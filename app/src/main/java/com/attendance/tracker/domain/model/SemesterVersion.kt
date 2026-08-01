package com.attendance.tracker.domain.model

/**
 * Domain model representing a timetable version for a semester.
 */
data class SemesterVersion(
    val id: Long = 0L,
    val name: String,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
