package com.attendance.tracker.data.local.datasource

import com.attendance.tracker.data.local.database.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import javax.inject.Inject

/**
 * Local Data Source interface defining interactions for attendance records.
 */
interface AttendanceLocalDataSource {
    /**
     * Exposes a Flow emitting attendance entities matching a study subject.
     */
    fun getAttendanceForSubject(subjectId: Long): Flow<List<AttendanceEntity>>

    /**
     * Exposes a Flow emitting attendance entities logged on a specific calendar date.
     */
    fun getAttendanceForDate(date: LocalDate): Flow<List<AttendanceEntity>>

    /**
     * Inserts or updates an attendance record entity.
     */
    suspend fun insertAttendance(attendance: AttendanceEntity): Long

    /**
     * Deletes an attendance record entity.
     */
    suspend fun deleteAttendance(attendance: AttendanceEntity)
}

/**
 * Local Data Source implementation providing skeleton stubs for attendance database operations.
 */
class AttendanceLocalDataSourceImpl @Inject constructor() : AttendanceLocalDataSource {
    override fun getAttendanceForSubject(subjectId: Long): Flow<List<AttendanceEntity>> = flowOf(emptyList())
    override fun getAttendanceForDate(date: LocalDate): Flow<List<AttendanceEntity>> = flowOf(emptyList())
    override suspend fun insertAttendance(attendance: AttendanceEntity): Long = 0L
    override suspend fun deleteAttendance(attendance: AttendanceEntity) {}
}
