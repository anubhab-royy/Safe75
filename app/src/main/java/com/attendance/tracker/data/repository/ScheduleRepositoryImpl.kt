package com.attendance.tracker.data.repository

import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.data.local.datasource.ScheduleLocalDataSource
import com.attendance.tracker.data.mapper.ScheduleMapper
import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of [ScheduleRepository] delegating to [ScheduleLocalDataSource].
 */
class ScheduleRepositoryImpl @Inject constructor(
    private val localDataSource: ScheduleLocalDataSource
) : ScheduleRepository {

    override fun getSchedules(): Flow<List<Schedule>> {
        return localDataSource.getAllSchedules().map { list ->
            list.map { ScheduleMapper.entityToDomain(it) }
        }
    }

    override fun getSchedulesForDay(day: WeekDay): Flow<List<Schedule>> {
        return localDataSource.getSchedulesForDay(day).map { list ->
            list.map { ScheduleMapper.entityToDomain(it) }
        }
    }

    override suspend fun insertSchedule(schedule: Schedule): Long {
        return localDataSource.insertSchedule(ScheduleMapper.domainToEntity(schedule))
    }

    override suspend fun deleteSchedule(schedule: Schedule) {
        localDataSource.deleteSchedule(ScheduleMapper.domainToEntity(schedule))
    }
}
