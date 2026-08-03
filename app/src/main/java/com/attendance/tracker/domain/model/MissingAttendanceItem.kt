package com.attendance.tracker.domain.model

import com.attendance.tracker.core.model.WeekDay
import java.time.LocalDate
import java.time.LocalTime

/**
 * Domain model representing a single expected class occurrence in the past
 * that has no attendance record yet.
 */
data class MissingAttendanceItem(
    val scheduleId: Long,
    val subjectId: Long,
    val date: LocalDate,
    val dayOfWeek: WeekDay,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val room: String? = null,
    val teacherOverride: String? = null
)
