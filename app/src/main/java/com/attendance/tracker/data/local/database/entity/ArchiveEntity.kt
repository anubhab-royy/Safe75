package com.attendance.tracker.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Database Entity representing a self-contained archived semester snapshot.
 * Stores serialized JSON for subjects, schedules, and attendance so the archive
 * remains valid even after a semester reset that removes live data.
 */
@Entity(tableName = "archives")
data class ArchiveEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    /** Display name chosen by the user when creating the archive. */
    val name: String,
    /** ISO-8601 date string of the semester start date. */
    val startDate: String,
    /** ISO-8601 date string of the semester end date. */
    val endDate: String,
    /** Epoch millis when the archive was created. */
    val archivedAt: Long = System.currentTimeMillis(),
    /** Serialized JSON array of [BackupSubjectDto]s at the time of archiving. */
    val subjectsJson: String,
    /** Serialized JSON array of [BackupScheduleDto]s at the time of archiving. */
    val schedulesJson: String,
    /** Serialized JSON array of [BackupAttendanceDto]s at the time of archiving. */
    val attendanceJson: String,
    /** Total scheduled classes in this archive period. */
    val totalClasses: Int = 0,
    /** Total classes marked PRESENT. */
    val presentCount: Int = 0,
    /** Total classes marked ABSENT. */
    val absentCount: Int = 0,
    /** Total classes marked CANCELLED. */
    val cancelledCount: Int = 0,
    /** Overall attendance percentage (present / (total - cancelled) * 100). */
    val overallPercentage: Double = 0.0
)
