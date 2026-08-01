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
import com.attendance.tracker.domain.repository.SubjectRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Worker identifying recently ended schedules without attendance records
 * and launching action-alert reminders.
 */
@HiltWorker
class PostClassReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val semesterRepository: SemesterRepository,
    private val scheduleRepository: ScheduleRepository,
    private val subjectRepository: SubjectRepository,
    private val attendanceRepository: AttendanceRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val enabled = settingsRepository.isNotificationsEnabled().first() &&
                settingsRepository.isAttendanceReminderEnabled().first()
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
        val subjects = subjectRepository.getSubjects().associateBy { it.id }
        val now = LocalTime.now()

        val recentSchedules = schedules.filter {
            it.endTime.isBefore(now) && it.endTime.isAfter(now.minusHours(2))
        }

        val todayLogs = attendanceRepository.getAttendanceForDateSync(LocalDate.now())

        recentSchedules.forEach { schedule ->
            val hasLog = todayLogs.any { it.scheduleId == schedule.id }
            if (!hasLog) {
                val subject = subjects[schedule.subjectId] ?: return@forEach
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                val timeSlot = "${schedule.startTime.format(formatter)} - ${schedule.endTime.format(formatter)}"
                TrackerNotificationManager.showPostClassReminder(
                    context = applicationContext,
                    scheduleId = schedule.id,
                    subjectId = schedule.subjectId,
                    subjectName = subject.name,
                    timeSlot = timeSlot
                )
            }
        }

        return Result.success()
    }
}
