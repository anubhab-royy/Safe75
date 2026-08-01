package com.attendance.tracker.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.core.notification.TrackerNotificationManager
import com.attendance.tracker.domain.repository.ScheduleRepository
import com.attendance.tracker.domain.repository.SemesterRepository
import com.attendance.tracker.domain.repository.SettingsRepository
import com.attendance.tracker.domain.repository.SubjectRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Worker compiling class timetables and launching morning summary alerts.
 */
@HiltWorker
class MorningReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val semesterRepository: SemesterRepository,
    private val scheduleRepository: ScheduleRepository,
    private val subjectRepository: SubjectRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val enabled = settingsRepository.isNotificationsEnabled().first() &&
                settingsRepository.isMorningReminderEnabled().first()
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

        val schedules = scheduleRepository.getSchedulesByDay(active.id, dayEnum).sortedBy { it.startTime }
        if (schedules.isEmpty()) return Result.success()

        val subjects = subjectRepository.getSubjects().associateBy { it.id }
        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        val summary = StringBuilder("Classes today:\n")
        schedules.forEach {
            val name = subjects[it.subjectId]?.name ?: "Subject"
            summary.append("- ${it.startTime.format(formatter)}: $name\n")
        }

        TrackerNotificationManager.showMorningReminder(applicationContext, summary.toString().trim())
        return Result.success()
    }
}
