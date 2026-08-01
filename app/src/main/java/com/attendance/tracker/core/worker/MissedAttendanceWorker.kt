package com.attendance.tracker.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.core.notification.TrackerNotificationManager
import com.attendance.tracker.domain.repository.AttendanceRepository
import com.attendance.tracker.domain.repository.ScheduleRepository
import com.attendance.tracker.domain.repository.SemesterRepository
import com.attendance.tracker.domain.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

/**
 * Worker checking for unmarked classes at end-of-day and posting summary alerts.
 */
@HiltWorker
class MissedAttendanceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val semesterRepository: SemesterRepository,
    private val scheduleRepository: ScheduleRepository,
    private val attendanceRepository: AttendanceRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val enabled = settingsRepository.isNotificationsEnabled().first() &&
                settingsRepository.isMissedReminderEnabled().first()
        if (!enabled) return Result.success()

        val active = semesterRepository.getActiveVersion() ?: return Result.success()

        val today = LocalDate.now().dayOfWeek
        val dayEnum = when (today) {
            java.time.DayOfWeek.MONDAY -> WeekDay.Monday
            java.time.DayOfWeek.TUESDAY -> WeekDay.Tuesday
            java.time.DayOfWeek.WEDNESDAY -> WeekDay.Wednesday
            java.time.DayOfWeek.THURSDAY -> WeekDay.Thursday
            java.time.DayOfWeek.FRIDAY -> WeekDay.Friday
            java.time.DayOfWeek.SATURDAY -> WeekDay.Saturday
            java.time.DayOfWeek.SUNDAY -> WeekDay.Sunday
        }

        val schedules = scheduleRepository.getSchedulesByDay(active.id, dayEnum)
        val now = LocalTime.now()

        // Find schedules that have finished today
        val finishedSchedules = schedules.filter { it.endTime.isBefore(now) }
        val todayLogs = attendanceRepository.getAttendanceForDateSync(LocalDate.now())

        var unmarkedCount = 0
        finishedSchedules.forEach { schedule ->
            val hasLog = todayLogs.any { it.scheduleId == schedule.id }
            if (!hasLog) {
                unmarkedCount++
            }
        }

        if (unmarkedCount > 0) {
            TrackerNotificationManager.showMissedReminder(applicationContext, unmarkedCount)
        }

        return Result.success()
    }
}
