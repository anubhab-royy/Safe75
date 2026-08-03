package com.attendance.tracker.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.tracker.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import com.attendance.tracker.core.model.AttendanceTarget
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsible for managing user configurations and preferences.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    /**
     * StateFlow representing the active user interface theme preference.
     */
    val themeState: StateFlow<String> = settingsRepository.getThemeMode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "SYSTEM"
        )

    /**
     * StateFlow representing whether local reminder notifications are enabled.
     */
    val notificationsState: StateFlow<Boolean> = settingsRepository.isNotificationsEnabled()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    /**
     * StateFlow representing the last database backup execution timestamp.
     */
    val lastBackupState: StateFlow<Long> = settingsRepository.getLastBackupTimestamp()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    val morningReminderState: StateFlow<Boolean> = settingsRepository.isMorningReminderEnabled()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val attendanceReminderState: StateFlow<Boolean> = settingsRepository.isAttendanceReminderEnabled()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val missedReminderState: StateFlow<Boolean> = settingsRepository.isMissedReminderEnabled()
         .stateIn(
             scope = viewModelScope,
             started = SharingStarted.WhileSubscribed(5000),
             initialValue = true
         )

    val attendanceGoal: StateFlow<Double> = settingsRepository.getAttendanceTarget()
        .map { it.personalGoal }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 75.0
        )

    fun setThemeMode(themeMode: String) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(themeMode)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
        }
    }

    fun setLastBackupTimestamp(timestamp: Long) {
        viewModelScope.launch {
            settingsRepository.setLastBackupTimestamp(timestamp)
        }
    }

    fun setMorningReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMorningReminderEnabled(enabled)
        }
    }

    fun setAttendanceReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAttendanceReminderEnabled(enabled)
        }
    }

    fun setMissedReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setMissedReminderEnabled(enabled)
        }
    }

    fun setAttendanceGoal(goal: Double) {
        viewModelScope.launch {
            settingsRepository.updateAttendanceTarget(AttendanceTarget(goal, goal))
        }
    }
}
