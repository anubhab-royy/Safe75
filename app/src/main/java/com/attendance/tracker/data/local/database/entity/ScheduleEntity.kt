package com.attendance.tracker.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.attendance.tracker.core.model.WeekDay
import java.time.LocalTime

/**
 * Database Entity representing weekly schedule configurations.
 * Contains relations and indexes to SubjectEntity and SemesterVersionEntity.
 */
@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SemesterVersionEntity::class,
            parentColumns = ["id"],
            childColumns = ["versionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["versionId"]),
        Index(value = ["versionId", "dayOfWeek"])
    ]
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val subjectId: Long,
    val dayOfWeek: WeekDay,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val room: String? = null,
    val teacherOverride: String? = null,
    val versionId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
