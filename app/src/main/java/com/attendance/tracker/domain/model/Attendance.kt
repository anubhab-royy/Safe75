package com.attendance.tracker.domain.model

import com.attendance.tracker.core.model.AttendanceStatus
import java.time.LocalDate

/**
 * Domain model representing a single attendance log.
 */
data class Attendance(
    val id: Long = 0L,
    val subjectId: Long,
    val scheduleId: Long,
    val date: LocalDate,
    val status: AttendanceStatus,
    val remarks: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
