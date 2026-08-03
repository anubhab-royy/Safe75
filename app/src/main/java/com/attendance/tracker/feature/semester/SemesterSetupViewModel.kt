package com.attendance.tracker.feature.semester

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.model.SemesterVersion
import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.domain.model.Subject
import com.attendance.tracker.core.model.AttendanceTarget
import com.attendance.tracker.domain.repository.AttendanceRepository
import com.attendance.tracker.domain.repository.ScheduleRepository
import com.attendance.tracker.domain.repository.SemesterRepository
import com.attendance.tracker.domain.repository.SubjectRepository
import com.attendance.tracker.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

enum class SetupStep {
    DETAILS,
    TIMETABLE,
    REVIEW_SCHEDULE,
    BACKFILL
}

data class PastClassItem(
    val date: LocalDate,
    val scheduleId: Long,
    val subjectId: Long,
    val subjectName: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val room: String?,
    val status: AttendanceStatus?
)

@HiltViewModel
class SemesterSetupViewModel @Inject constructor(
    private val semesterRepository: SemesterRepository,
    private val settingsRepository: SettingsRepository,
    private val scheduleRepository: ScheduleRepository,
    private val attendanceRepository: AttendanceRepository,
    private val subjectRepository: SubjectRepository
) : ViewModel() {

    private val _currentStep = MutableStateFlow(SetupStep.DETAILS)
    val currentStep = _currentStep.asStateFlow()

    private val _semesterName = MutableStateFlow("Semester 1")
    val semesterName = _semesterName.asStateFlow()

    private val _startDate = MutableStateFlow(LocalDate.now())
    val startDate = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow(LocalDate.now().plusMonths(4))
    val endDate = _endDate.asStateFlow()

    private val _attendanceGoal = MutableStateFlow(75)
    val attendanceGoal = _attendanceGoal.asStateFlow()

    private val _createdSemesterId = MutableStateFlow<Long?>(null)
    val createdSemesterId = _createdSemesterId.asStateFlow()

    private val _subjects = MutableStateFlow<Map<Long, Subject>>(emptyMap())
    private val _schedules = MutableStateFlow<List<Schedule>>(emptyList())
    private val _attendance = MutableStateFlow<List<Attendance>>(emptyList())

    init {
        viewModelScope.launch {
            runCatching {
                val target = settingsRepository.getAttendanceTarget().first()
                _attendanceGoal.value = target.personalGoal.toInt()
            }
        }

        viewModelScope.launch {
            subjectRepository.observeSubjects().collect { list ->
                _subjects.value = list.associateBy { it.id }
            }
        }

        viewModelScope.launch {
            _createdSemesterId.collect { id ->
                if (id != null) {
                    scheduleRepository.observeSchedulesForVersion(id).collect { list ->
                        _schedules.value = list
                    }
                }
            }
        }

        viewModelScope.launch {
            attendanceRepository.observeAttendanceHistory().collect { list ->
                _attendance.value = list
            }
        }
    }

    val pastClassesList: StateFlow<List<PastClassItem>> = combine(
        _startDate,
        _endDate,
        _schedules,
        _subjects,
        _attendance
    ) { start, end, schedules, subjects, attendance ->
        val today = LocalDate.now()
        val limit = if (end.isBefore(today)) end else today
        if (start.isAfter(limit) || schedules.isEmpty()) {
            emptyList()
        } else {
            val list = mutableListOf<PastClassItem>()
            var date = start
            while (!date.isAfter(limit)) {
                val dayOfWeek = date.dayOfWeek
                val weekDay = when (dayOfWeek) {
                    java.time.DayOfWeek.MONDAY -> WeekDay.Monday
                    java.time.DayOfWeek.TUESDAY -> WeekDay.Tuesday
                    java.time.DayOfWeek.WEDNESDAY -> WeekDay.Wednesday
                    java.time.DayOfWeek.THURSDAY -> WeekDay.Thursday
                    java.time.DayOfWeek.FRIDAY -> WeekDay.Friday
                    java.time.DayOfWeek.SATURDAY -> WeekDay.Saturday
                    java.time.DayOfWeek.SUNDAY -> WeekDay.Sunday
                }
                val dailySchedules = schedules.filter { it.dayOfWeek == weekDay }
                dailySchedules.forEach { schedule ->
                    val subject = subjects[schedule.subjectId]
                    val subjectName = subject?.name ?: "Unknown"
                    val record = attendance.find { it.scheduleId == schedule.id && it.date == date }
                    list.add(
                        PastClassItem(
                            date = date,
                            scheduleId = schedule.id,
                            subjectId = schedule.subjectId,
                            subjectName = subjectName,
                            startTime = schedule.startTime,
                            endTime = schedule.endTime,
                            room = schedule.room,
                            status = record?.status
                        )
                    )
                }
                date = date.plusDays(1)
            }
            list.sortedWith(compareBy<PastClassItem> { it.date }.thenBy { it.startTime })
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val schedulesList: StateFlow<List<Schedule>> = _schedules

    fun setStep(step: SetupStep) {
        _currentStep.value = step
    }

    fun setSemesterName(name: String) {
        _semesterName.value = name
    }

    fun setStartDate(date: LocalDate) {
        _startDate.value = date
        if (_endDate.value.isBefore(date)) {
            _endDate.value = date.plusMonths(4)
        }
    }

    fun setEndDate(date: LocalDate) {
        _endDate.value = date
    }

    fun setAttendanceGoal(goal: Int) {
        _attendanceGoal.value = goal
    }

    fun createSemester() {
        viewModelScope.launch {
            val name = _semesterName.value.ifBlank { "Semester 1" }
            val start = _startDate.value
            val end = _endDate.value
            val goal = _attendanceGoal.value.toDouble()

            settingsRepository.updateAttendanceTarget(
                AttendanceTarget(requiredPercentage = goal, personalGoal = goal)
            )

            val version = SemesterVersion(
                name = name,
                isActive = true,
                startDate = start,
                endDate = end
            )
            val newId = semesterRepository.insertVersion(version)
            semesterRepository.switchActiveVersion(newId)
            _createdSemesterId.value = newId
            _currentStep.value = SetupStep.TIMETABLE
        }
    }

    fun markPastAttendance(item: PastClassItem, status: AttendanceStatus) {
        viewModelScope.launch {
            val record = _attendance.value.find { it.scheduleId == item.scheduleId && it.date == item.date }
            if (record != null) {
                if (record.status == status) {
                    attendanceRepository.deleteAttendance(record)
                } else {
                    attendanceRepository.updateAttendance(record.copy(status = status, updatedAt = System.currentTimeMillis()))
                }
            } else {
                val newRecord = Attendance(
                    subjectId = item.subjectId,
                    scheduleId = item.scheduleId,
                    date = item.date,
                    status = status
                )
                attendanceRepository.insertAttendance(newRecord)
            }
        }
    }
}
