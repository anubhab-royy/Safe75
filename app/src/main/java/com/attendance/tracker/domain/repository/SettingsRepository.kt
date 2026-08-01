package com.attendance.tracker.domain.repository

import com.attendance.tracker.core.model.AttendanceTarget
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining operations for reading and writing user configurations.
 */
interface SettingsRepository {
    /**
     * Exposes a Flow emitting the persistent theme mode string.
     */
    fun getThemeMode(): Flow<String>

    /**
     * Updates the theme mode configuration.
     */
    suspend fun setThemeMode(themeMode: String)

    /**
     * Exposes a Flow emitting whether notifications are enabled.
     */
    fun isNotificationsEnabled(): Flow<Boolean>

    /**
     * Updates the notifications enablement state.
     */
    suspend fun setNotificationsEnabled(enabled: Boolean)

    /**
     * Exposes a Flow emitting the timestamp of the last local/cloud database backup.
     */
    fun getLastBackupTimestamp(): Flow<Long>

    /**
     * Updates the backup timestamp.
     */
    suspend fun setLastBackupTimestamp(timestamp: Long)

    /**
     * Exposes a Flow emitting the current attendance requirements and goals.
     */
    fun getAttendanceTarget(): Flow<AttendanceTarget>

    /**
     * Updates the target attendance constraints.
     */
    suspend fun updateAttendanceTarget(target: AttendanceTarget)
}
