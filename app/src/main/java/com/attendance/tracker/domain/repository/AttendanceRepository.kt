package com.attendance.tracker.domain.repository

import com.attendance.tracker.domain.model.AttendanceRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository interface defining operations for logging and analyzing attendance records.
 */
interface AttendanceRepository {
    /**
     * Exposes a Flow emitting all attendance logs for a specific subject.
     */
    fun getAttendanceRecordsForSubject(subjectId: Long): Flow<List<AttendanceRecord>>

    /**
     * Exposes a Flow emitting attendance logs registered on a specific calendar date.
     */
    fun getAttendanceRecordsForDate(date: LocalDate): Flow<List<AttendanceRecord>>

    /**
     * Inserts or updates an attendance log entry, returning its database ID.
     */
    suspend fun insertAttendanceRecord(record: AttendanceRecord): Long

    /**
     * Removes an attendance entry.
     */
    suspend fun deleteAttendanceRecord(record: AttendanceRecord)
}
