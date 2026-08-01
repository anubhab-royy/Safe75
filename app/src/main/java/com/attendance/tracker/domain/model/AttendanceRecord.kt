package com.attendance.tracker.domain.model

import com.attendance.tracker.core.model.AttendanceStatus
import java.time.LocalDate

/**
 * Domain model representing a single attendance log event.
 */
data class AttendanceRecord(
    val id: Long = 0L,
    val subjectId: Long,
    val date: LocalDate,
    val status: AttendanceStatus,
    val note: String? = null
)
