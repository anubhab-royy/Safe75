package com.attendance.tracker.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.attendance.tracker.core.model.AttendanceStatus
import java.time.LocalDate

/**
 * Database Entity representing a single attendance log event.
 * Links to SubjectEntity and ScheduleEntity with cascade delete rules,
 * and maintains a unique constraint on (subjectId, scheduleId, date) to prevent duplicate records.
 */
@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["scheduleId"]),
        Index(value = ["subjectId", "scheduleId", "date"], unique = true)
    ]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val subjectId: Long,
    val scheduleId: Long,
    val date: LocalDate,
    val status: AttendanceStatus,
    val remarks: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
