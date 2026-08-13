package com.attendance.tracker.domain.repository

import com.attendance.tracker.domain.model.Attendance
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository interface defining operations for logging and analyzing attendance records.
 */
interface AttendanceRepository {

    fun observeAttendanceForSubject(subjectId: Long): Flow<List<Attendance>>

    fun observeAttendanceForDate(date: LocalDate): Flow<List<Attendance>>

    fun observeTodayAttendance(date: LocalDate): Flow<List<Attendance>>

    fun observeAttendanceHistory(): Flow<List<Attendance>>

    fun observeAttendanceById(id: Long): Flow<Attendance?>

    suspend fun getAttendanceById(id: Long): Attendance?

    suspend fun getAttendanceForSubject(subjectId: Long): List<Attendance>

    suspend fun getAttendanceForDateSync(date: LocalDate): List<Attendance>

    suspend fun insertAttendance(attendance: Attendance): Long

    suspend fun updateAttendance(attendance: Attendance): Int

    suspend fun deleteAttendance(attendance: Attendance): Int

    suspend fun checkDuplicateAttendance(subjectId: Long, scheduleId: Long, date: LocalDate): Boolean

    fun countPresent(): Flow<Int>

    fun countAbsent(): Flow<Int>

    fun countCancelled(): Flow<Int>

    fun searchAttendance(query: String): Flow<List<Attendance>>

    suspend fun saveOcrAttendanceBatch(records: List<Attendance>): Int
}
