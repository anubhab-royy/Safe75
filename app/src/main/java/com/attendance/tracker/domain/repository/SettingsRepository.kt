package com.attendance.tracker.domain.repository

import com.attendance.tracker.core.model.AttendanceTarget
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining operations for reading and writing user configurations.
 */
interface SettingsRepository {
    fun getThemeMode(): Flow<String>
    suspend fun setThemeMode(themeMode: String)

    fun isNotificationsEnabled(): Flow<Boolean>
    suspend fun setNotificationsEnabled(enabled: Boolean)

    fun getLastBackupTimestamp(): Flow<Long>
    suspend fun setLastBackupTimestamp(timestamp: Long)

    fun getAttendanceTarget(): Flow<AttendanceTarget>
    suspend fun updateAttendanceTarget(target: AttendanceTarget)

    // Reminders
    fun isMorningReminderEnabled(): Flow<Boolean>
    suspend fun setMorningReminderEnabled(enabled: Boolean)

    fun isAttendanceReminderEnabled(): Flow<Boolean>
    suspend fun setAttendanceReminderEnabled(enabled: Boolean)

    fun isMissedReminderEnabled(): Flow<Boolean>
    suspend fun setMissedReminderEnabled(enabled: Boolean)
}
