package com.attendance.tracker.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Database Entity representing a study subject.
 * Annotated with [Entity] for Room schema configuration.
 */
@Entity(
    tableName = "subjects",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val facultyName: String? = null,
    val color: Int = 0,
    val requiredAttendancePercentage: Int = 75,
    val personalAttendanceGoal: Int = 85,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
