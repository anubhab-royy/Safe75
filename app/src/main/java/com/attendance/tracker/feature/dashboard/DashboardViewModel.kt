package com.attendance.tracker.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.model.DashboardStatistics
import com.attendance.tracker.domain.model.PlannerResult
import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.domain.model.SemesterVersion
import com.attendance.tracker.domain.model.SimulationResult
import com.attendance.tracker.domain.model.Subject
import com.attendance.tracker.domain.model.SubjectStatistics
import com.attendance.tracker.domain.repository.AttendanceRepository
import com.attendance.tracker.domain.repository.ScheduleRepository
import com.attendance.tracker.domain.repository.SemesterRepository
import com.attendance.tracker.domain.repository.SubjectRepository
import com.attendance.tracker.domain.usecase.attendance.CalculateAttendanceStatisticsUseCase
import com.attendance.tracker.domain.usecase.attendance.GetTodayAttendanceUseCase
import com.attendance.tracker.domain.usecase.planner.AttendanceSimulatorUseCase
import com.attendance.tracker.domain.usecase.planner.LeavePlannerUseCase
import com.attendance.tracker.domain.usecase.attendance.MarkAttendanceUseCase
import com.attendance.tracker.domain.usecase.attendance.UpdateAttendanceUseCase
import com.attendance.tracker.domain.usecase.attendance.DeleteAttendanceUseCase
import com.attendance.tracker.feature.attendance.TodayScheduleItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import com.attendance.tracker.domain.repository.SettingsRepository


/**
 * ViewModel acting as the intelligence hub for analytics, simulator runs, and leaves.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val subjectRepository: SubjectRepository,
    private val scheduleRepository: ScheduleRepository,
    private val semesterRepository: SemesterRepository,
    private val calculateStatisticsUseCase: CalculateAttendanceStatisticsUseCase,
    private val getTodayAttendanceUseCase: GetTodayAttendanceUseCase,
    private val attendanceSimulatorUseCase: AttendanceSimulatorUseCase,
    private val leavePlannerUseCase: LeavePlannerUseCase,
    private val markAttendanceUseCase: MarkAttendanceUseCase,
    private val updateAttendanceUseCase: UpdateAttendanceUseCase,
    private val deleteAttendanceUseCase: DeleteAttendanceUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val attendanceGoal: StateFlow<Double> = settingsRepository.getAttendanceTarget()
        .map { it.personalGoal }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 75.0
        )

    private val _subjectsMap = MutableStateFlow<Map<Long, Subject>>(emptyMap())
    val subjectsMap = _subjectsMap.asStateFlow()

    private val _schedulesMap = MutableStateFlow<Map<Long, Schedule>>(emptyMap())

    private val _activeVersion = MutableStateFlow<SemesterVersion?>(null)
    val activeVersion = _activeVersion.asStateFlow()

    // Simulation states
    private val _simPresentInput = MutableStateFlow(0)
    val simPresentInput = _simPresentInput.asStateFlow()

    private val _simAbsentInput = MutableStateFlow(0)
    val simAbsentInput = _simAbsentInput.asStateFlow()

    // Leave states
    private val _selectedLeaveDates = MutableStateFlow<List<LocalDate>>(emptyList())
    val selectedLeaveDates = _selectedLeaveDates.asStateFlow()

    init {
        viewModelScope.launch {
            subjectRepository.observeSubjects().collect { list ->
                _subjectsMap.value = list.associateBy { it.id }
            }
        }

        viewModelScope.launch {
            semesterRepository.observeActiveVersion().collect { active ->
                _activeVersion.value = active
                if (active != null) {
                    scheduleRepository.observeSchedulesForVersion(active.id).collect { list ->
                        _schedulesMap.value = list.associateBy { it.id }
                    }
                }
            }
        }
    }

    /**
     * Today's classes state flow mapped with recorded attendance status.
     */
    val todayClasses: StateFlow<List<TodayScheduleItem>> = combine(
        _activeVersion,
        _schedulesMap,
        getTodayAttendanceUseCase(),
        _subjectsMap
    ) { activeVer, schedules, todayAttendance, subjects ->
        if (activeVer == null) return@combine emptyList()

        val currentDay = LocalDate.now().dayOfWeek
        val dayEnum = when (currentDay) {
            java.time.DayOfWeek.MONDAY -> WeekDay.Monday
            java.time.DayOfWeek.TUESDAY -> WeekDay.Tuesday
            java.time.DayOfWeek.WEDNESDAY -> WeekDay.Wednesday
            java.time.DayOfWeek.THURSDAY -> WeekDay.Thursday
            java.time.DayOfWeek.FRIDAY -> WeekDay.Friday
            java.time.DayOfWeek.SATURDAY -> WeekDay.Saturday
            java.time.DayOfWeek.SUNDAY -> WeekDay.Sunday
        }

        val nowTime = java.time.LocalTime.now()
        schedules.values
            .filter { it.dayOfWeek == dayEnum }
            .map { schedule ->
                val subject = subjects[schedule.subjectId]
                val attendance = todayAttendance.find { it.scheduleId == schedule.id }
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                TodayScheduleItem(
                    scheduleId = schedule.id,
                    subjectId = schedule.subjectId,
                    subjectName = subject?.name ?: "Unknown Subject",
                    subjectColor = subject?.color ?: 0xFF9E9E9E.toInt(),
                    startTime = schedule.startTime.format(formatter),
                    endTime = schedule.endTime.format(formatter),
                    room = schedule.room,
                    faculty = schedule.teacherOverride ?: subject?.facultyName,
                    attendance = attendance
                )
            }
            .sortedWith(compareBy<TodayScheduleItem> { item ->
                val sched = schedules[item.scheduleId] ?: return@compareBy 3
                val isMarked = item.attendance != null
                val isCurrent = !isMarked && nowTime >= sched.startTime && nowTime <= sched.endTime
                val isUpcoming = !isMarked && nowTime < sched.startTime
                if (isCurrent) 1 else if (isUpcoming) 2 else 3
            }.thenBy { item ->
                val sched = schedules[item.scheduleId]
                sched?.startTime ?: java.time.LocalTime.MIDNIGHT
            })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    /**
     * Handles immediate attendance state toggling for a today schedule class item.
     */
    fun onAttendanceStatusClick(item: TodayScheduleItem, newStatus: AttendanceStatus) {
        viewModelScope.launch {
            val existing = item.attendance
            if (existing == null) {
                // Not marked -> insert new attendance
                val record = Attendance(
                    subjectId = item.subjectId,
                    scheduleId = item.scheduleId,
                    date = LocalDate.now(),
                    status = newStatus
                )
                markAttendanceUseCase(record)
            } else {
                if (existing.status == newStatus) {
                    // Clicked again -> toggle off (delete) to return to unselected
                    deleteAttendanceUseCase(existing)
                } else {
                    // Change status -> update attendance
                    val updated = existing.copy(
                        status = newStatus,
                        updatedAt = System.currentTimeMillis()
                    )
                    updateAttendanceUseCase(updated)
                }
            }
        }
    }

    /**
     * Attendance history logs.
     */
    val filteredHistory: StateFlow<List<Attendance>> = combine(
        attendanceRepository.observeAttendanceHistory(),
        activeVersion,
        _schedulesMap
    ) { history, activeVer, schedulesMap ->
        if (activeVer == null) emptyList()
        else {
            history.filter { record ->
                val isWithinDateRange = !record.date.isBefore(activeVer.startDate) && !record.date.isAfter(activeVer.endDate)
                val belongsToActiveSchedule = record.scheduleId == 0L || schedulesMap.containsKey(record.scheduleId)
                isWithinDateRange && belongsToActiveSchedule
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    /**
     * Dashboard statistics.
     */
    val dashboardStats: StateFlow<DashboardStatistics?> = combine(
        filteredHistory,
        _subjectsMap,
        attendanceGoal
    ) { history, _, goal ->
        val gInt = goal.toInt()
        val stats = calculateStatisticsUseCase(history, gInt, gInt)
        val status = when {
            stats.totalClasses == 0 -> "NEUTRAL"
            stats.attendancePercentage < goal -> "CRITICAL"
            stats.remainingSafeClasses == 0 -> "WARNING"
            else -> "GOOD"
        }
        DashboardStatistics(
            overallPercentage = stats.attendancePercentage,
            presentCount = stats.presentCount,
            absentCount = stats.absentCount,
            cancelledCount = stats.cancelledCount,
            totalClasses = stats.totalClasses,
            safetyStatus = status,
            safeMissCount = stats.remainingSafeClasses,
            classesNeeded = stats.classesNeededToReachGoal
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    /**
     * Subject stats list.
     */
    val subjectStatsList: StateFlow<List<SubjectStatistics>> = combine(
        filteredHistory,
        _subjectsMap,
        attendanceGoal
    ) { history, subjects, goal ->
        val gInt = goal.toInt()
        subjects.values.map { subject ->
            val subjectHistory = history.filter { it.subjectId == subject.id }
            val stats = calculateStatisticsUseCase(
                subjectHistory,
                gInt,
                gInt
            )
            val status = when {
                stats.attendancePercentage < goal -> "CRITICAL"
                stats.remainingSafeClasses == 0 -> "WARNING"
                else -> "GOOD"
            }
            SubjectStatistics(
                subjectId = subject.id,
                subjectName = subject.name,
                subjectColor = subject.color,
                presentCount = stats.presentCount,
                totalClasses = stats.totalClasses,
                percentage = stats.attendancePercentage,
                requiredPercentage = goal.toInt(),
                personalGoalPercentage = goal.toInt(),
                safetyStatus = status,
                safeMissCount = stats.remainingSafeClasses,
                classesNeeded = stats.classesNeededToReachGoal
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    /**
     * Next scheduled class today that has not started yet.
     */
    val upcomingClass: StateFlow<TodayScheduleItem?> = combine(
        _activeVersion,
        _schedulesMap,
        getTodayAttendanceUseCase(),
        _subjectsMap
    ) { activeVer, schedules, todayAttendance, subjects ->
        if (activeVer == null) return@combine null

        val currentDay = LocalDate.now().dayOfWeek
        val dayEnum = when (currentDay) {
            java.time.DayOfWeek.MONDAY -> WeekDay.Monday
            java.time.DayOfWeek.TUESDAY -> WeekDay.Tuesday
            java.time.DayOfWeek.WEDNESDAY -> WeekDay.Wednesday
            java.time.DayOfWeek.THURSDAY -> WeekDay.Thursday
            java.time.DayOfWeek.FRIDAY -> WeekDay.Friday
            java.time.DayOfWeek.SATURDAY -> WeekDay.Saturday
            java.time.DayOfWeek.SUNDAY -> WeekDay.Sunday
        }

        val nowTime = LocalTime.now()
        schedules.values
            .filter { it.dayOfWeek == dayEnum && it.startTime.isAfter(nowTime) }
            .minByOrNull { it.startTime }
            .let { schedule ->
                if (schedule == null) return@let null
                val subject = subjects[schedule.subjectId]
                val attendance = todayAttendance.find { it.scheduleId == schedule.id }
                val formatter = DateTimeFormatter.ofPattern("HH:mm")
                TodayScheduleItem(
                    scheduleId = schedule.id,
                    subjectId = schedule.subjectId,
                    subjectName = subject?.name ?: "Unknown Subject",
                    subjectColor = subject?.color ?: 0xFF9E9E9E.toInt(),
                    startTime = schedule.startTime.format(formatter),
                    endTime = schedule.endTime.format(formatter),
                    room = schedule.room,
                    faculty = schedule.teacherOverride ?: subject?.facultyName,
                    attendance = attendance
                )
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )


    // Simulator triggers
    val simulationResult: StateFlow<SimulationResult?> = combine(
        attendanceRepository.observeAttendanceHistory(),
        _simPresentInput,
        _simAbsentInput
    ) { history, simP, simA ->
        val present = history.count { it.status == AttendanceStatus.PRESENT }
        val absent = history.count { it.status == AttendanceStatus.ABSENT }
        attendanceSimulatorUseCase(present, absent, simP, simA)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    fun onSimPresentChange(count: Int) {
        _simPresentInput.value = count
    }

    fun onSimAbsentChange(count: Int) {
        _simAbsentInput.value = count
    }

    // Leave planner triggers
    val leavePlannerResult: StateFlow<PlannerResult?> = combine(
        _selectedLeaveDates,
        _activeVersion
    ) { dates, activeVer ->
        if (activeVer == null || dates.isEmpty()) {
            null
        } else {
            leavePlannerUseCase(dates, activeVer.id)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    fun toggleLeaveDate(date: LocalDate) {
        val current = _selectedLeaveDates.value.toMutableList()
        if (current.contains(date)) {
            current.remove(date)
        } else {
            current.add(date)
        }
        _selectedLeaveDates.value = current
    }

    fun clearLeaveDates() {
        _selectedLeaveDates.value = emptyList()
    }
}
