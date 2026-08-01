package com.attendance.tracker.domain.repository

import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.domain.model.Schedule
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining data operations for weekly schedules.
 */
interface ScheduleRepository {
    /**
     * Exposes a Flow emitting all schedules.
     */
    fun getSchedules(): Flow<List<Schedule>>

    /**
     * Exposes a Flow emitting schedules matching a specific weekday.
     */
    fun getSchedulesForDay(day: WeekDay): Flow<List<Schedule>>

    /**
     * Inserts or updates a schedule entry, returning its database ID.
     */
    suspend fun insertSchedule(schedule: Schedule): Long

    /**
     * Removes a schedule entry from the database.
     */
    suspend fun deleteSchedule(schedule: Schedule)
}
