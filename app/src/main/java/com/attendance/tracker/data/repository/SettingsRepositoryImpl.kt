package com.attendance.tracker.data.repository

import com.attendance.tracker.core.model.AttendanceTarget
import com.attendance.tracker.data.local.datasource.SettingsLocalDataSource
import com.attendance.tracker.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
        // Returns default target requirements initially
        return flowOf(AttendanceTarget(75.0, 85.0))
    }

    override suspend fun updateAttendanceTarget(target: AttendanceTarget) {
        // Mock execution for phase 1. Detailed persistence will be implemented next.
    }
}
