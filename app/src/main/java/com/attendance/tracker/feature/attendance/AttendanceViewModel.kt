package com.attendance.tracker.feature.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.data.mapper.AttendanceMapper
import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.model.AttendanceStatistics
import com.attendance.tracker.domain.model.AttendanceSummary
import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.domain.model.SemesterVersion
import com.attendance.tracker.domain.model.Subject
import com.attendance.tracker.domain.repository.ScheduleRepository
import com.attendance.tracker.domain.repository.SemesterRepository
import com.attendance.tracker.domain.repository.SubjectRepository
import com.attendance.tracker.domain.usecase.attendance.CalculateAttendanceStatisticsUseCase
import com.attendance.tracker.domain.usecase.attendance.DeleteAttendanceUseCase
import com.attendance.tracker.domain.usecase.attendance.GetAttendanceHistoryUseCase
import com.attendance.tracker.domain.usecase.attendance.GetTodayAttendanceUseCase
import com.attendance.tracker.domain.usecase.attendance.MarkAttendanceUseCase
import com.attendance.tracker.domain.usecase.attendance.UpdateAttendanceUseCase
import com.attendance.tracker.domain.validation.ValidationResult
import com.attendance.tracker.feature.attendance.model.AttendanceUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * UI representation of today's schedule items mapped with their logged attendance status.
 */
data class TodayScheduleItem(
    val scheduleId: Long,
    val subjectId: Long,
    val subjectName: String,
    val subjectColor: Int,
    val startTime: String,
    val endTime: String,
    val room: String?,
    val faculty: String?,
    val attendance: Attendance? = null
)

/**
 * ViewModel managing the active attendance tracking workflow.
 */
@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val markAttendanceUseCase: MarkAttendanceUseCase,
    private val updateAttendanceUseCase: UpdateAttendanceUseCase,
    private val deleteAttendanceUseCase: DeleteAttendanceUseCase,
    private val getAttendanceHistoryUseCase: GetAttendanceHistoryUseCase,
    private val getTodayAttendanceUseCase: GetTodayAttendanceUseCase,
    private val calculateAttendanceStatisticsUseCase: CalculateAttendanceStatisticsUseCase,
    private val subjectRepository: SubjectRepository,
    private val scheduleRepository: ScheduleRepository,
    private val semesterRepository: SemesterRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedSubjectId = MutableStateFlow<Long?>(null)
    val selectedSubjectId = _selectedSubjectId.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate = _selectedDate.asStateFlow()

    private val _validationState = MutableStateFlow<ValidationResult>(ValidationResult.Valid)
    val validationState = _validationState.asStateFlow()

    private val _subjectsMap = MutableStateFlow<Map<Long, Subject>>(emptyMap())
    val subjectsMap = _subjectsMap.asStateFlow()

    private val _schedulesMap = MutableStateFlow<Map<Long, Schedule>>(emptyMap())

    private val _activeVersion = MutableStateFlow<SemesterVersion?>(null)
    val activeVersion = _activeVersion.asStateFlow()

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
     * Today's classes state flow.
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

        schedules.values
            .filter { it.dayOfWeek == dayEnum }
            .sortedBy { it.startTime }
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
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    private val _filters = combine(
        _searchQuery,
        _selectedSubjectId,
        _selectedDate
    ) { query, subId, date ->
        Triple(query, subId, date)
    }

    /**
     * History records with reactive search/filter combinations.
     */
    val historyRecords: StateFlow<List<AttendanceUiModel>> = combine(
        getAttendanceHistoryUseCase(),
        _filters,
        _subjectsMap,
        _schedulesMap
    ) { rawRecords, filters, subjects, schedules ->
        val (query, subId, date) = filters
        rawRecords.map { record ->
            val subject = subjects[record.subjectId]
            val schedule = schedules[record.scheduleId]
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val timeRange = if (schedule != null) {
                "${schedule.startTime.format(formatter)} - ${schedule.endTime.format(formatter)}"
            } else {
                ""
            }
            AttendanceMapper.domainToUi(
                domain = record,
                subjectName = subject?.name ?: "Unknown Subject",
                subjectColor = subject?.color ?: 0xFF9E9E9E.toInt(),
                timeRange = timeRange
            )
        }.filter { uiModel ->
            (subId == null || uiModel.subjectId == subId) &&
            (date == null || uiModel.date == date.toString()) &&
            (query.isBlank() || uiModel.subjectName.contains(query, ignoreCase = true) ||
                    (uiModel.remarks?.contains(query, ignoreCase = true) == true))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    /**
     * Overall calculated statistics.
     */
    val overallStats: StateFlow<AttendanceStatistics?> = combine(
        getAttendanceHistoryUseCase(),
        _subjectsMap
    ) { history, _ ->
        if (history.isEmpty()) {
            AttendanceStatistics(
                presentCount = 0,
                absentCount = 0,
                cancelledCount = 0,
                medicalLeaveCount = 0,
                totalClasses = 0,
                attendancePercentage = 100.0,
                withMedicalPercentage = 100.0,
                remainingSafeClasses = 0,
                classesNeededToReachGoal = 0
            )
        } else {
            calculateAttendanceStatisticsUseCase(history, 75, 85)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    /**
     * Stats summarized for each subject separately.
     */
    val subjectSummaries: StateFlow<List<AttendanceSummary>> = combine(
        getAttendanceHistoryUseCase(),
        _subjectsMap
    ) { history, subjects ->
        subjects.values.map { subject ->
            val recordsForSubject = history.filter { it.subjectId == subject.id }
            val stats = calculateAttendanceStatisticsUseCase(
                recordsForSubject,
                subject.requiredAttendancePercentage,
                subject.personalAttendanceGoal
            )
            AttendanceSummary(
                subjectId = subject.id,
                subjectName = subject.name,
                subjectColor = subject.color,
                statistics = stats,
                requiredAttendancePercentage = subject.requiredAttendancePercentage,
                personalAttendanceGoal = subject.personalAttendanceGoal
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onSubjectFilterChange(subjectId: Long?) {
        _selectedSubjectId.value = subjectId
    }

    fun onDateFilterChange(date: LocalDate?) {
        _selectedDate.value = date
    }

    fun clearValidationError() {
        _validationState.value = ValidationResult.Valid
    }

    /**
     * Logs attendance for a specific class slot.
     */
    suspend fun markAttendance(
        subjectId: Long,
        scheduleId: Long,
        status: AttendanceStatus,
        remarks: String? = null,
        date: LocalDate = LocalDate.now()
    ): Boolean {
        clearValidationError()
        val record = Attendance(
            subjectId = subjectId,
            scheduleId = scheduleId,
            date = date,
            status = status,
            remarks = remarks?.trim()?.takeIf { it.isNotBlank() }
        )
        val result = markAttendanceUseCase(record)
        if (result is ValidationResult.Invalid) {
            _validationState.value = result
            return false
        }
        return true
    }

    /**
     * Modifies attendance status and optional remarks note.
     */
    suspend fun updateAttendance(
        id: Long,
        subjectId: Long,
        scheduleId: Long,
        date: LocalDate,
        status: AttendanceStatus,
        remarks: String?
    ): Boolean {
        clearValidationError()
        val record = Attendance(
            id = id,
            subjectId = subjectId,
            scheduleId = scheduleId,
            date = date,
            status = status,
            remarks = remarks?.trim()?.takeIf { it.isNotBlank() }
        )
        val result = updateAttendanceUseCase(record)
        if (result is ValidationResult.Invalid) {
            _validationState.value = result
            return false
        }
        return true
    }

    /**
     * Wipes an attendance entry from the database.
     */
    fun deleteAttendance(id: Long, subjectId: Long, scheduleId: Long, date: LocalDate, status: AttendanceStatus, remarks: String?) {
        viewModelScope.launch {
            val record = Attendance(
                id = id,
                subjectId = subjectId,
                scheduleId = scheduleId,
                date = date,
                status = status,
                remarks = remarks
            )
            deleteAttendanceUseCase(record)
        }
    }

    suspend fun getAttendanceById(id: Long): Attendance? {
        return getAttendanceHistoryUseCase().stateIn(viewModelScope).value.find { it.id == id }
    }
}
