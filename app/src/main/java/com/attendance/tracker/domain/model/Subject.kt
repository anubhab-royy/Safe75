package com.attendance.tracker.domain.model

/**
 * Domain model representing a study subject.
 */
data class Subject(
    val id: Long = 0L,
    val name: String,
    val code: String? = null,
    val creditHours: Int = 0
)
