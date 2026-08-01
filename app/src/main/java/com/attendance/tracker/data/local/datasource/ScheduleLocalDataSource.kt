package com.attendance.tracker.data.local.datasource

import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.data.local.database.dao.ScheduleDao
import com.attendance.tracker.data.local.database.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime
import javax.inject.Inject

/**
 * Local Data Source interface defining interactions for class schedules.
 */
interface ScheduleLocalDataSource {
    fun observeSchedulesForVersion(versionId: Long): Flow<List<ScheduleEntity>>
    fun observeSchedule(id: Long): Flow<ScheduleEntity?>
    suspend fun getSchedule(id: Long): ScheduleEntity?
    suspend fun getSchedulesForVersion(versionId: Long): List<ScheduleEntity>
    fun observeTodaySchedules(versionId: Long, day: WeekDay): Flow<List<ScheduleEntity>>
    suspend fun getSchedulesBySubject(subjectId: Long): List<ScheduleEntity>
    suspend fun getSchedulesByDay(versionId: Long, day: WeekDay): List<ScheduleEntity>
    suspend fun searchSchedules(versionId: Long, query: String): List<ScheduleEntity>
    suspend fun checkConflicts(versionId: Long, day: WeekDay, start: LocalTime, end: LocalTime): List<ScheduleEntity>
    suspend fun insertSchedule(schedule: ScheduleEntity): Long
    suspend fun updateSchedule(schedule: ScheduleEntity): Int
    suspend fun deleteSchedule(schedule: ScheduleEntity): Int
}

/**
 * Local Data Source implementation providing hooks for schedule database operations.
 */
class ScheduleLocalDataSourceImpl @Inject constructor(
    private val scheduleDao: ScheduleDao
) : ScheduleLocalDataSource {
    override fun observeSchedulesForVersion(versionId: Long): Flow<List<ScheduleEntity>> = scheduleDao.observeSchedulesForVersion(versionId)
    override fun observeSchedule(id: Long): Flow<ScheduleEntity?> = scheduleDao.observeSchedule(id)
    override suspend fun getSchedule(id: Long): ScheduleEntity? = scheduleDao.getSchedule(id)
    override suspend fun getSchedulesForVersion(versionId: Long): List<ScheduleEntity> = scheduleDao.getSchedulesForVersion(versionId)
    override fun observeTodaySchedules(versionId: Long, day: WeekDay): Flow<List<ScheduleEntity>> = scheduleDao.observeTodaySchedules(versionId, day)
    override suspend fun getSchedulesBySubject(subjectId: Long): List<ScheduleEntity> = scheduleDao.getSchedulesBySubject(subjectId)
    override suspend fun getSchedulesByDay(versionId: Long, day: WeekDay): List<ScheduleEntity> = scheduleDao.getSchedulesByDay(versionId, day)
    override suspend fun searchSchedules(versionId: Long, query: String): List<ScheduleEntity> = scheduleDao.searchSchedules(versionId, query)
    override suspend fun checkConflicts(versionId: Long, day: WeekDay, start: LocalTime, end: LocalTime): List<ScheduleEntity> = scheduleDao.checkConflicts(versionId, day, start, end)
    override suspend fun insertSchedule(schedule: ScheduleEntity): Long = scheduleDao.insertSchedule(schedule)
    override suspend fun updateSchedule(schedule: ScheduleEntity): Int = scheduleDao.updateSchedule(schedule)
    override suspend fun deleteSchedule(schedule: ScheduleEntity): Int = scheduleDao.deleteSchedule(schedule)
}
