package com.attendance.tracker.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.attendance.tracker.core.model.WeekDay
import java.time.LocalTime

/**
 * Database Entity representing weekly schedule configurations.
 */
@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val subjectId: Long,
    val dayOfWeek: WeekDay,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val room: String? = null
)
