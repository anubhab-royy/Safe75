package com.attendance.tracker.data.local.datasource

import com.attendance.tracker.data.local.preferences.SettingsPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Local Data Source interface defining preference store interactions.
 */
interface SettingsLocalDataSource {
    /**
     * Exposes a Flow emitting the theme mode configurations.
     */
    fun getThemeMode(): Flow<String>

    /**
     * Persists the theme mode.
     */
    suspend fun setThemeMode(themeMode: String)

    /**
     * Exposes a Flow emitting notification status.
     */
    fun isNotificationsEnabled(): Flow<Boolean>

    /**
     * Persists the notification enabled/disabled state.
     */
    suspend fun setNotificationsEnabled(enabled: Boolean)

    /**
     * Exposes a Flow emitting the last backup run timestamp.
     */
    fun getLastBackupTimestamp(): Flow<Long>

    /**
     * Persists the last backup timestamp.
     */
    suspend fun setLastBackupTimestamp(timestamp: Long)
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
}
