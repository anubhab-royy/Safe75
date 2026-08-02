package com.attendance.tracker.data.local.datasource

import android.content.Context
import com.attendance.tracker.core.widget.WidgetRefreshScheduler
import com.attendance.tracker.data.local.database.dao.AttendanceDao
import com.attendance.tracker.data.local.database.entity.AttendanceEntity
import dagger.hilt.android.qualifiers.ApplicationContext
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
 *
 * Every successful mutation schedules a home screen widget refresh so the
 * widget reflects the latest attendance state (see WidgetSyncWorker).
 */
class AttendanceLocalDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
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
        val id = attendanceDao.insert(attendance)
        if (id > 0) refreshWidget()
        return id
    }

    override suspend fun updateAttendance(attendance: AttendanceEntity): Int {
        val updated = attendanceDao.update(attendance)
        if (updated > 0) refreshWidget()
        return updated
    }

    override suspend fun deleteAttendance(attendance: AttendanceEntity): Int {
        val deleted = attendanceDao.delete(attendance)
        if (deleted > 0) refreshWidget()
        return deleted
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

    private fun refreshWidget() {
        WidgetRefreshScheduler.schedule(context)
    }
}
