package com.attendance.tracker.domain.repository

import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.domain.model.Schedule
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

/**
 * Repository interface defining data operations for weekly schedules.
 */
interface ScheduleRepository {
    fun observeSchedulesForVersion(versionId: Long): Flow<List<Schedule>>
    fun observeSchedule(id: Long): Flow<Schedule?>
    suspend fun getSchedule(id: Long): Schedule?
    suspend fun getSchedulesForVersion(versionId: Long): List<Schedule>
    fun observeTodaySchedules(versionId: Long, day: WeekDay): Flow<List<Schedule>>
    suspend fun getSchedulesBySubject(subjectId: Long): List<Schedule>
    suspend fun getSchedulesByDay(versionId: Long, day: WeekDay): List<Schedule>
    suspend fun searchSchedules(versionId: Long, query: String): List<Schedule>
    suspend fun checkConflicts(versionId: Long, day: WeekDay, start: LocalTime, end: LocalTime): List<Schedule>
    suspend fun insertSchedule(schedule: Schedule): Long
    suspend fun updateSchedule(schedule: Schedule): Int
    suspend fun deleteSchedule(schedule: Schedule): Int
}
