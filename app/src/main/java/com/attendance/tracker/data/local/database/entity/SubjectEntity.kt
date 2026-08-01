package com.attendance.tracker.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database Entity representing a study subject.
 * Annotated with [Entity] for Room schema configuration.
 */
@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val code: String? = null,
    val creditHours: Int = 0
)
