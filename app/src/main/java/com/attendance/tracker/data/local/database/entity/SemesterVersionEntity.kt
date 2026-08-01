package com.attendance.tracker.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database Entity representing academic timetable versions.
 */
@Entity(tableName = "semester_versions")
data class SemesterVersionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
