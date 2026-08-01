package com.attendance.tracker.data.local.datasource

import com.attendance.tracker.data.local.database.dao.AttendanceDao
import com.attendance.tracker.data.local.database.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/**
 * Local Data Source interface defining interactions for attendance records.
 */
interface AttendanceLocalDataSource {

    fun observeAttendanceForSubject(subjectId: Long): Flow<List<AttendanceEntity>>

    fun observeAttendanceForDate(date: LocalDate): Flow<List<AttendanceEntity>>

    fun observeTodayAttendance(date: LocalDate): Flow<List<AttendanceEntity>>

    fun observeAttendanceHistory(): Flow<List<AttendanceEntity>>

    fun observeAttendanceById(id: Long): Flow<AttendanceEntity?>

    suspend fun getAttendanceById(id: Long): AttendanceEntity?

    suspend fun getAttendanceForDateSync(date: LocalDate): List<AttendanceEntity>

    suspend fun insertAttendance(attendance: AttendanceEntity): Long

    suspend fun updateAttendance(attendance: AttendanceEntity): Int

    suspend fun deleteAttendance(attendance: AttendanceEntity): Int

    suspend fun checkDuplicateAttendance(subjectId: Long, scheduleId: Long, date: LocalDate): Boolean

    fun countPresent(): Flow<Int>

    fun countAbsent(): Flow<Int>

    fun countCancelled(): Flow<Int>

    fun searchAttendance(query: String): Flow<List<AttendanceEntity>>
}

/**
 * Local Data Source implementation delegating directly to [AttendanceDao].
 */
class AttendanceLocalDataSourceImpl @Inject constructor(
    private val attendanceDao: AttendanceDao
) : AttendanceLocalDataSource {

    override fun observeAttendanceForSubject(subjectId: Long): Flow<List<AttendanceEntity>> {
        return attendanceDao.getAttendanceBySubject(subjectId)
    }

    override fun observeAttendanceForDate(date: LocalDate): Flow<List<AttendanceEntity>> {
        return attendanceDao.getAttendanceByDate(date)
    }

    override fun observeTodayAttendance(date: LocalDate): Flow<List<AttendanceEntity>> {
        return attendanceDao.observeTodayAttendance(date)
    }

    override fun observeAttendanceHistory(): Flow<List<AttendanceEntity>> {
        return attendanceDao.getAttendanceHistory()
    }

    override fun observeAttendanceById(id: Long): Flow<AttendanceEntity?> {
        return attendanceDao.observeAttendanceById(id)
    }

    override suspend fun getAttendanceById(id: Long): AttendanceEntity? {
        return attendanceDao.getAttendanceById(id)
    }

    override suspend fun getAttendanceForDateSync(date: LocalDate): List<AttendanceEntity> {
        return attendanceDao.getAttendanceByDateSync(date)
    }

    override suspend fun insertAttendance(attendance: AttendanceEntity): Long {
        return attendanceDao.insert(attendance)
    }

    override suspend fun updateAttendance(attendance: AttendanceEntity): Int {
        return attendanceDao.update(attendance)
    }

    override suspend fun deleteAttendance(attendance: AttendanceEntity): Int {
        return attendanceDao.delete(attendance)
    }

    override suspend fun checkDuplicateAttendance(subjectId: Long, scheduleId: Long, date: LocalDate): Boolean {
        return attendanceDao.checkDuplicateAttendance(subjectId, scheduleId, date)
    }

    override fun countPresent(): Flow<Int> {
        return attendanceDao.countPresent()
    }

    override fun countAbsent(): Flow<Int> {
        return attendanceDao.countAbsent()
    }

    override fun countCancelled(): Flow<Int> {
        return attendanceDao.countCancelled()
    }

    override fun searchAttendance(query: String): Flow<List<AttendanceEntity>> {
        return attendanceDao.searchAttendance(query)
    }
}
