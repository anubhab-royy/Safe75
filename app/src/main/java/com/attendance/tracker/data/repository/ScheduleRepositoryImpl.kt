package com.attendance.tracker.data.repository

import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.data.local.datasource.ScheduleLocalDataSource
import com.attendance.tracker.data.mapper.ScheduleMapper
import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import javax.inject.Inject

/**
 * Implementation of [ScheduleRepository] wrapping local database datasource queries.
 */
class ScheduleRepositoryImpl @Inject constructor(
    private val localDataSource: ScheduleLocalDataSource
) : ScheduleRepository {

    override fun observeSchedulesForVersion(versionId: Long): Flow<List<Schedule>> {
        return localDataSource.observeSchedulesForVersion(versionId).map { list ->
            list.map { ScheduleMapper.entityToDomain(it) }
        }
    }

    override fun observeSchedule(id: Long): Flow<Schedule?> {
        return localDataSource.observeSchedule(id).map { entity ->
            entity?.let { ScheduleMapper.entityToDomain(it) }
        }
    }

    override suspend fun getSchedule(id: Long): Schedule? {
        return localDataSource.getSchedule(id)?.let { ScheduleMapper.entityToDomain(it) }
    }

    override suspend fun getSchedulesForVersion(versionId: Long): List<Schedule> {
        return localDataSource.getSchedulesForVersion(versionId).map { ScheduleMapper.entityToDomain(it) }
    }

    override fun observeTodaySchedules(versionId: Long, day: WeekDay): Flow<List<Schedule>> {
        return localDataSource.observeTodaySchedules(versionId, day).map { list ->
            list.map { ScheduleMapper.entityToDomain(it) }
        }
    }

    override suspend fun getSchedulesBySubject(subjectId: Long): List<Schedule> {
        return localDataSource.getSchedulesBySubject(subjectId).map { ScheduleMapper.entityToDomain(it) }
    }

    override suspend fun getSchedulesByDay(versionId: Long, day: WeekDay): List<Schedule> {
        return localDataSource.getSchedulesByDay(versionId, day).map { ScheduleMapper.entityToDomain(it) }
    }

    override suspend fun searchSchedules(versionId: Long, query: String): List<Schedule> {
        return localDataSource.searchSchedules(versionId, query).map { ScheduleMapper.entityToDomain(it) }
    }

    override suspend fun checkConflicts(
        versionId: Long,
        day: WeekDay,
        start: LocalTime,
        end: LocalTime
    ): List<Schedule> {
        return localDataSource.checkConflicts(versionId, day, start, end).map { ScheduleMapper.entityToDomain(it) }
    }

    override suspend fun insertSchedule(schedule: Schedule): Long {
        return localDataSource.insertSchedule(ScheduleMapper.domainToEntity(schedule))
    }

    override suspend fun updateSchedule(schedule: Schedule): Int {
        return localDataSource.updateSchedule(ScheduleMapper.domainToEntity(schedule))
    }

    override suspend fun deleteSchedule(schedule: Schedule): Int {
        return localDataSource.deleteSchedule(ScheduleMapper.domainToEntity(schedule))
    }
}
