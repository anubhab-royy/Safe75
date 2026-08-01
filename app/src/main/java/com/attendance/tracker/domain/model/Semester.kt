package com.attendance.tracker.domain.model

import java.time.LocalDate

/**
 * Domain model representing an academic semester.
 */
data class Semester(
    val id: Long = 0L,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate
)
