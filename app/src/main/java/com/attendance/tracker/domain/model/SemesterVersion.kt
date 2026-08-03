package com.attendance.tracker.domain.model

import java.time.LocalDate

/**
 * Domain model representing a timetable version for a semester.
 */
data class SemesterVersion(
    val id: Long = 0L,
    val name: String,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate = LocalDate.now().plusMonths(4)
)
