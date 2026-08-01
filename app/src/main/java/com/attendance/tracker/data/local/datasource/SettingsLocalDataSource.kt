package com.attendance.tracker.data.local.datasource

import com.attendance.tracker.data.local.preferences.SettingsPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Local Data Source interface defining preference store interactions.
 */
interface SettingsLocalDataSource {
    fun getThemeMode(): Flow<String>
    suspend fun setThemeMode(themeMode: String)

    fun isNotificationsEnabled(): Flow<Boolean>
    suspend fun setNotificationsEnabled(enabled: Boolean)

    fun getLastBackupTimestamp(): Flow<Long>
    suspend fun setLastBackupTimestamp(timestamp: Long)

    // Reminders
    fun isMorningReminderEnabled(): Flow<Boolean>
    suspend fun setMorningReminderEnabled(enabled: Boolean)

    fun isAttendanceReminderEnabled(): Flow<Boolean>
    suspend fun setAttendanceReminderEnabled(enabled: Boolean)

    fun isMissedReminderEnabled(): Flow<Boolean>
    suspend fun setMissedReminderEnabled(enabled: Boolean)
}

/**
 * Local Data Source implementation wrapping [SettingsPreferences] operations.
 */
class SettingsLocalDataSourceImpl @Inject constructor(
    private val settingsPreferences: SettingsPreferences
) : SettingsLocalDataSource {
    override fun getThemeMode(): Flow<String> = settingsPreferences.themeFlow
    override suspend fun setThemeMode(themeMode: String) = settingsPreferences.setThemeMode(themeMode)
    override fun isNotificationsEnabled(): Flow<Boolean> = settingsPreferences.notificationsFlow
    override suspend fun setNotificationsEnabled(enabled: Boolean) = settingsPreferences.setNotificationsEnabled(enabled)
    override fun getLastBackupTimestamp(): Flow<Long> = settingsPreferences.lastBackupFlow
    override suspend fun setLastBackupTimestamp(timestamp: Long) = settingsPreferences.setLastBackupTimestamp(timestamp)

    // Reminders implementation
    override fun isMorningReminderEnabled(): Flow<Boolean> = settingsPreferences.morningReminderFlow
    override suspend fun setMorningReminderEnabled(enabled: Boolean) = settingsPreferences.setMorningReminderEnabled(enabled)
    override fun isAttendanceReminderEnabled(): Flow<Boolean> = settingsPreferences.attendanceReminderFlow
    override suspend fun setAttendanceReminderEnabled(enabled: Boolean) = settingsPreferences.setAttendanceReminderEnabled(enabled)
    override fun isMissedReminderEnabled(): Flow<Boolean> = settingsPreferences.missedReminderFlow
    override suspend fun setMissedReminderEnabled(enabled: Boolean) = settingsPreferences.setMissedReminderEnabled(enabled)
}
