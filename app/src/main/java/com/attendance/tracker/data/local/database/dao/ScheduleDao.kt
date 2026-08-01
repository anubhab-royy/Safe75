package com.attendance.tracker.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.data.local.database.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

/**
 * Data Access Object for Room database operations on class schedules.
 */
@Dao
interface ScheduleDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity): Int

    @Delete
    suspend fun deleteSchedule(schedule: ScheduleEntity): Int

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getSchedule(id: Long): ScheduleEntity?

    @Query("SELECT * FROM schedules WHERE id = :id")
    fun observeSchedule(id: Long): Flow<ScheduleEntity?>

    @Query("SELECT * FROM schedules WHERE versionId = :versionId ORDER BY dayOfWeek ASC, startTime ASC")
    fun observeSchedulesForVersion(versionId: Long): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE versionId = :versionId ORDER BY dayOfWeek ASC, startTime ASC")
    suspend fun getSchedulesForVersion(versionId: Long): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE versionId = :versionId AND dayOfWeek = :day ORDER BY startTime ASC")
    fun observeTodaySchedules(versionId: Long, day: WeekDay): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE subjectId = :subjectId ORDER BY dayOfWeek ASC, startTime ASC")
    suspend fun getSchedulesBySubject(subjectId: Long): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE versionId = :versionId AND dayOfWeek = :day ORDER BY startTime ASC")
    suspend fun getSchedulesByDay(versionId: Long, day: WeekDay): List<ScheduleEntity>

    /**
     * Search schedules matching queries against room, teacherOverride, or joined subject details.
     * Direct query on schedules matching query text.
     */
    @Query("SELECT * FROM schedules WHERE versionId = :versionId AND (room LIKE '%' || :query || '%' OR teacherOverride LIKE '%' || :query || '%') ORDER BY dayOfWeek ASC, startTime ASC")
    suspend fun searchSchedules(versionId: Long, query: String): List<ScheduleEntity>

    /**
     * Finds conflicting schedules on the same day within a time overlap.
     * Overlap checks: (StartA < EndB) AND (EndA > StartB)
     */
    @Query("SELECT * FROM schedules WHERE versionId = :versionId AND dayOfWeek = :day AND startTime < :end AND endTime > :start")
    suspend fun checkConflicts(
        versionId: Long,
        day: WeekDay,
        start: LocalTime,
        end: LocalTime
    ): List<ScheduleEntity>
}
