package com.attendance.tracker.domain.model

import com.attendance.tracker.core.model.WeekDay
import java.time.LocalTime

/**
 * Domain model representing a schedule configuration for a subject.
 */
data class Schedule(
    val id: Long = 0L,
    val subjectId: Long,
    val dayOfWeek: WeekDay,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val room: String? = null,
    val teacherOverride: String? = null,
    val versionId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
