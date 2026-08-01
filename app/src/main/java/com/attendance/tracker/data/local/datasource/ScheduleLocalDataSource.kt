package com.attendance.tracker.data.local.datasource

import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.data.local.database.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Local Data Source interface defining interactions for class schedules.
 */
interface ScheduleLocalDataSource {
    /**
     * Exposes a Flow emitting all schedule entities.
     */
    fun getAllSchedules(): Flow<List<ScheduleEntity>>

    /**
     * Exposes a Flow emitting schedule entities corresponding to a weekday.
     */
    fun getSchedulesForDay(day: WeekDay): Flow<List<ScheduleEntity>>

    /**
     * Inserts or updates a schedule config entity.
     */
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    /**
     * Deletes a schedule config entity.
     */
    suspend fun deleteSchedule(schedule: ScheduleEntity)
}

/**
 * Local Data Source implementation providing skeleton stubs for schedule database operations.
 */
class ScheduleLocalDataSourceImpl @Inject constructor() : ScheduleLocalDataSource {
    override fun getAllSchedules(): Flow<List<ScheduleEntity>> = flowOf(emptyList())
    override fun getSchedulesForDay(day: WeekDay): Flow<List<ScheduleEntity>> = flowOf(emptyList())
    override suspend fun insertSchedule(schedule: ScheduleEntity): Long = 0L
    override suspend fun deleteSchedule(schedule: ScheduleEntity) {}
}
