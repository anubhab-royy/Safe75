package com.attendance.tracker.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.attendance.tracker.core.model.AttendanceStatus
import java.time.LocalDate

/**
 * Database Entity representing a single attendance log.
 */
@Entity(tableName = "attendance_records")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val subjectId: Long,
    val date: LocalDate,
    val status: AttendanceStatus,
    val note: String? = null
)
