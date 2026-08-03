package com.attendance.tracker.data.repository

import com.attendance.tracker.core.model.AttendanceTarget
import com.attendance.tracker.data.local.datasource.SettingsLocalDataSource
import com.attendance.tracker.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of [SettingsRepository] delegating preference storage to [SettingsLocalDataSource].
 */
class SettingsRepositoryImpl @Inject constructor(
    private val localDataSource: SettingsLocalDataSource
) : SettingsRepository {

    override fun getThemeMode(): Flow<String> = localDataSource.getThemeMode()

    override suspend fun setThemeMode(themeMode: String) {
        localDataSource.setThemeMode(themeMode)
    }

    override fun isNotificationsEnabled(): Flow<Boolean> = localDataSource.isNotificationsEnabled()

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        localDataSource.setNotificationsEnabled(enabled)
    }

    override fun getLastBackupTimestamp(): Flow<Long> = localDataSource.getLastBackupTimestamp()

    override suspend fun setLastBackupTimestamp(timestamp: Long) {
        localDataSource.setLastBackupTimestamp(timestamp)
    }

    override fun getAttendanceTarget(): Flow<AttendanceTarget> {
        return localDataSource.getAttendanceGoal().map { goal ->
            AttendanceTarget(goal, goal)
        }
    }

    override suspend fun updateAttendanceTarget(target: AttendanceTarget) {
        localDataSource.setAttendanceGoal(target.personalGoal)
    }

    // Reminders
    override fun isMorningReminderEnabled(): Flow<Boolean> = localDataSource.isMorningReminderEnabled()
    override suspend fun setMorningReminderEnabled(enabled: Boolean) = localDataSource.setMorningReminderEnabled(enabled)

    override fun isAttendanceReminderEnabled(): Flow<Boolean> = localDataSource.isAttendanceReminderEnabled()
    override suspend fun setAttendanceReminderEnabled(enabled: Boolean) = localDataSource.setAttendanceReminderEnabled(enabled)

    override fun isMissedReminderEnabled(): Flow<Boolean> = localDataSource.isMissedReminderEnabled()
    override suspend fun setMissedReminderEnabled(enabled: Boolean) = localDataSource.setMissedReminderEnabled(enabled)
}
