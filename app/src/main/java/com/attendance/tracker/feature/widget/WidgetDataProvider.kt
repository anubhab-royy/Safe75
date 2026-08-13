package com.attendance.tracker.feature.widget

import com.attendance.tracker.core.common.DispatcherProvider
import com.attendance.tracker.core.logger.Logger
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.domain.repository.AttendanceRepository
import com.attendance.tracker.domain.repository.ScheduleRepository
import com.attendance.tracker.domain.repository.SemesterRepository
import com.attendance.tracker.domain.repository.SubjectRepository
import com.attendance.tracker.domain.usecase.attendance.CalculateAttendanceStatisticsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the [WidgetSummary] used by the home screen widget.
 *
 * Reads are performed on a background thread so the widget render and the
 * WorkManager refresh worker never block the main thread.
 */
@Singleton
class WidgetDataProvider @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val scheduleRepository: ScheduleRepository,
    private val semesterRepository: SemesterRepository,
    private val subjectRepository: SubjectRepository,
    private val statisticsUseCase: CalculateAttendanceStatisticsUseCase,
    private val dispatcherProvider: DispatcherProvider
) {

    /**
     * Computes a fresh snapshot of the widget data.
     */
    suspend fun load(): WidgetSummary = withContext(dispatcherProvider.io) {
        try {
            val activeVersion = semesterRepository.getActiveVersion()
            val subjects = subjectRepository.getSubjects().associateBy { it.id }
            val history = attendanceRepository.observeAttendanceHistory().first()
            val stats = statisticsUseCase(history, 75, 85)

            val today = LocalDate.now()
            val todaySchedules = if (activeVersion != null) {
                scheduleRepository.getSchedulesByDay(activeVersion.id, today.dayOfWeek.toWeekDay())
                    .sortedBy { it.startTime }
            } else {
                emptyList()
            }
            val todayLogs = attendanceRepository.getAttendanceForDateSync(today)

            val next = todaySchedules
                .firstOrNull { it.startTime.isAfter(LocalTime.now()) }
            val formatter = DateTimeFormatter.ofPattern("HH:mm")

            WidgetSummary(
                overallPercentage = stats.attendancePercentage,
                todayClasses = todaySchedules.size,
                todayLogged = todayLogs.size,
                nextClassSubject = next?.let { subjects[it.subjectId]?.name },
                nextClassStartTime = next?.startTime?.format(formatter),
                nextClassEndTime = next?.endTime?.format(formatter),
                nextClassRoom = next?.room,
                hasData = subjects.isNotEmpty() || history.isNotEmpty()
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to load widget summary", e)
            WidgetSummary.EMPTY
        }
    }

    private fun java.time.DayOfWeek.toWeekDay(): WeekDay = when (this) {
        java.time.DayOfWeek.MONDAY -> WeekDay.Monday
        java.time.DayOfWeek.TUESDAY -> WeekDay.Tuesday
        java.time.DayOfWeek.WEDNESDAY -> WeekDay.Wednesday
        java.time.DayOfWeek.THURSDAY -> WeekDay.Thursday
        java.time.DayOfWeek.FRIDAY -> WeekDay.Friday
        java.time.DayOfWeek.SATURDAY -> WeekDay.Saturday
        java.time.DayOfWeek.SUNDAY -> WeekDay.Sunday
    }

    private companion object {
        const val TAG = "WidgetDataProvider"
    }
}
