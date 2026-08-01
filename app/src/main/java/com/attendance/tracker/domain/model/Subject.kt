package com.attendance.tracker.domain.model

/**
 * Domain model representing a study subject.
 */
data class Subject(
    val id: Long = 0L,
    val name: String,
    val facultyName: String? = null,
    val color: Int = 0,
    val requiredAttendancePercentage: Int = 75,
    val personalAttendanceGoal: Int = 85,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
