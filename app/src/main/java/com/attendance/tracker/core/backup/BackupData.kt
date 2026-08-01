package com.attendance.tracker.core.backup

import kotlinx.serialization.Serializable

/**
 * Serializable DTO representing a [com.attendance.tracker.domain.model.Subject].
 * All time values are preserved as raw epoch millis.
 */
@Serializable
data class BackupSubjectDto(
    val id: Long,
    val name: String,
    val facultyName: String? = null,
    val color: Int = 0,
    val requiredAttendancePercentage: Int = 75,
    val personalAttendanceGoal: Int = 85,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Serializable DTO representing a [com.attendance.tracker.domain.model.Schedule].
 * [dayOfWeek] is stored as the enum name string for portability.
 * [startTime] and [endTime] are ISO-8601 local time strings (HH:mm:ss).
 */
@Serializable
data class BackupScheduleDto(
    val id: Long,
    val subjectId: Long,
    val dayOfWeek: String,
    val startTime: String,
    val endTime: String,
    val room: String? = null,
    val teacherOverride: String? = null,
    val versionId: Long,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Serializable DTO representing a [com.attendance.tracker.domain.model.SemesterVersion].
 */
@Serializable
data class BackupSemesterDto(
    val id: Long,
    val name: String,
    val isActive: Boolean = false,
    val createdAt: Long
)

/**
 * Serializable DTO representing a [com.attendance.tracker.domain.model.Attendance].
 * [date] is ISO-8601 date string (yyyy-MM-dd).
 * [status] is the enum name string.
 */
@Serializable
data class BackupAttendanceDto(
    val id: Long,
    val subjectId: Long,
    val scheduleId: Long,
    val date: String,
    val status: String,
    val remarks: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Serializable DTO representing persisted user settings at backup time.
 */
@Serializable
data class BackupSettingsDto(
    val themeMode: String = "SYSTEM",
    val notificationsEnabled: Boolean = true,
    val morningReminderEnabled: Boolean = true,
    val attendanceReminderEnabled: Boolean = true,
    val missedReminderEnabled: Boolean = true
)

/**
 * Root backup payload combining [BackupMetadata] with all domain data DTOs.
 * This is the exact object serialized to and from the exported JSON file.
 */
@Serializable
data class BackupData(
    val metadata: BackupMetadata = BackupMetadata(),
    val subjects: List<BackupSubjectDto> = emptyList(),
    val schedules: List<BackupScheduleDto> = emptyList(),
    val semesterVersions: List<BackupSemesterDto> = emptyList(),
    val attendanceRecords: List<BackupAttendanceDto> = emptyList(),
    val settings: BackupSettingsDto = BackupSettingsDto()
)
