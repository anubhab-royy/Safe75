package com.attendance.tracker.data.local.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SettingsPreferences wraps Preferences DataStore to persist user settings.
 */
@Singleton
class SettingsPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val KEY_LAST_BACKUP = longPreferencesKey("last_backup_timestamp")
        
        // Future preference keys
        val KEY_REQUIRED_ATTENDANCE = stringPreferencesKey("required_attendance_percentage")
        val KEY_PERSONAL_GOAL_ATTENDANCE = stringPreferencesKey("personal_goal_attendance_percentage")
    }

    /**
     * Emits the selected theme mode string ("SYSTEM", "LIGHT", or "DARK").
     */
    val themeFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[KEY_THEME] ?: "SYSTEM"
    }

    /**
     * Emits whether user notifications are enabled.
     */
    val notificationsFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS] ?: true
    }

    /**
     * Emits the timestamp in milliseconds since epoch of the last backup run.
     */
    val lastBackupFlow: Flow<Long> = dataStore.data.map { preferences ->
        preferences[KEY_LAST_BACKUP] ?: 0L
    }

    /**
     * Persists the selected theme mode.
     */
    suspend fun setThemeMode(themeMode: String) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME] = themeMode
        }
    }

    /**
     * Persists the user notifications enable status.
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS] = enabled
        }
    }

    /**
     * Persists the timestamp when the database was backed up.
     */
    suspend fun setLastBackupTimestamp(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_BACKUP] = timestamp
        }
    }
}
