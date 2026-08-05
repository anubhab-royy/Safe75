package com.attendance.tracker.feature.schedule

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.tracker.core.common.UiState
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.data.mapper.ScheduleMapper
import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.domain.model.SemesterVersion
import com.attendance.tracker.domain.repository.AttendanceRepository
import com.attendance.tracker.domain.repository.ScheduleRepository
import com.attendance.tracker.domain.repository.SemesterRepository
import com.attendance.tracker.domain.repository.SubjectRepository
import com.attendance.tracker.domain.usecase.attendance.DetectMissingAttendanceUseCase
import com.attendance.tracker.domain.usecase.schedule.AddScheduleUseCase
import com.attendance.tracker.domain.usecase.schedule.DeleteScheduleUseCase
import com.attendance.tracker.domain.usecase.schedule.DetectConflictUseCase
import com.attendance.tracker.domain.usecase.schedule.GetWeekScheduleUseCase
import com.attendance.tracker.domain.usecase.schedule.ObserveScheduleUseCase
import com.attendance.tracker.domain.usecase.schedule.SaveMultiDayScheduleUseCase
import com.attendance.tracker.domain.usecase.schedule.SwitchTimetableVersionUseCase
import com.attendance.tracker.domain.usecase.schedule.UpdateScheduleUseCase
import com.attendance.tracker.domain.validation.ScheduleValidator
import com.attendance.tracker.domain.validation.ValidationResult
import com.attendance.tracker.feature.schedule.model.ScheduleUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * Encapsulates a suggested backfill action surfaced after saving a schedule.
 */
data class BackfillPrompt(
    val subjectId: Long,
    val subjectName: String,
    val missingCount: Int
)

/**
 * ViewModel managing the active weekly schedules, conflicts, and timetable version configurations.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val observeScheduleUseCase: ObserveScheduleUseCase,
    private val getWeekScheduleUseCase: GetWeekScheduleUseCase,
    private val addScheduleUseCase: AddScheduleUseCase,
    private val updateScheduleUseCase: UpdateScheduleUseCase,
    private val deleteScheduleUseCase: DeleteScheduleUseCase,
    private val saveMultiDayScheduleUseCase: SaveMultiDayScheduleUseCase,
    private val detectConflictUseCase: DetectConflictUseCase,
    private val switchTimetableVersionUseCase: SwitchTimetableVersionUseCase,
    private val semesterRepository: SemesterRepository,
    private val subjectRepository: SubjectRepository,
    private val scheduleRepository: ScheduleRepository,
    private val attendanceRepository: AttendanceRepository,
    private val detectMissingAttendanceUseCase: DetectMissingAttendanceUseCase,
    private val validator: ScheduleValidator
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _versions = MutableStateFlow<List<SemesterVersion>>(emptyList())
    val versions = _versions.asStateFlow()

    private val _activeVersion = MutableStateFlow<SemesterVersion?>(null)
    val activeVersion = _activeVersion.asStateFlow()

    private val _schedules = MutableStateFlow<List<ScheduleUiModel>>(emptyList())
    val schedules = _schedules.asStateFlow()

    private val _validationState = MutableStateFlow<ValidationResult>(ValidationResult.Valid)
    val validationState = _validationState.asStateFlow()

    private val _conflicts = MutableStateFlow<List<Schedule>>(emptyList())
    val conflicts = _conflicts.asStateFlow()

    private val _pendingBackfillPrompt = MutableStateFlow<BackfillPrompt?>(null)
    val pendingBackfillPrompt = _pendingBackfillPrompt.asStateFlow()

    private val _subjects = MutableStateFlow<List<com.attendance.tracker.domain.model.Subject>>(emptyList())
    val subjects = _subjects.asStateFlow()

    val uiState: StateFlow<UiState<List<ScheduleUiModel>>> = combine(
        _schedules,
        _searchQuery
    ) { list, query ->
        if (list.isEmpty() && query.isBlank()) {
            UiState.Empty
        } else {
            UiState.Success(list)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UiState.Loading
    )

    init {
        initVersions()
        viewModelScope.launch {
            _subjects.value = subjectRepository.getSubjects()
        }

        viewModelScope.launch {
            _activeVersion
                .flatMapLatest { active ->
                    if (active != null) {
                        combine(
                            observeScheduleUseCase(active.id),
                            _searchQuery
                        ) { raw, query ->
                            val subjectsMap = subjectRepository.getSubjects().associateBy { it.id }
                            val mapped = raw.map { schedule ->
                                val subject = subjectsMap[schedule.subjectId]
                                ScheduleMapper.domainToUi(
                                    domain = schedule,
                                    subjectName = subject?.name ?: "Unknown Subject",
                                    subjectColor = subject?.color ?: 0xFF9E9E9E.toInt(),
                                    subjectFaculty = subject?.facultyName
                                )
                            }

                            if (query.isBlank()) {
                                mapped
                            } else {
                                mapped.filter {
                                    it.subjectName.contains(query, ignoreCase = true) ||
                                            (it.faculty?.contains(query, ignoreCase = true) == true) ||
                                            (it.room?.contains(query, ignoreCase = true) == true)
                                }
                            }
                        }
                    } else {
                        flowOf(emptyList())
                    }
                }
                .catch {
                    emit(emptyList())
                }
                .collect { list ->
                    _schedules.value = list
                }
        }
    }

    private fun initVersions() {
        viewModelScope.launch {
            semesterRepository.observeActiveVersion().collect { active ->
                if (active != null) {
                    _activeVersion.value = active
                } else {
                    val all = semesterRepository.getVersions()
                    if (all.isEmpty()) {
                        val defaultId = semesterRepository.insertVersion(
                            SemesterVersion(name = "Semester 1", isActive = true)
                        )
                        semesterRepository.switchActiveVersion(defaultId)
                    } else {
                        semesterRepository.switchActiveVersion(all.first().id)
                    }
                }
            }
        }

        viewModelScope.launch {
            semesterRepository.observeVersions().collect { list ->
                _versions.value = list
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun clearValidationError() {
        _validationState.value = ValidationResult.Valid
        _conflicts.value = emptyList()
    }

    /**
     * Attempts to save a schedule block.
     * Checks validation rules and scans for conflicts.
     * Returns true if saved, false otherwise.
     */
    suspend fun saveSchedule(
        id: Long = 0L,
        subjectId: Long,
        day: WeekDay,
        startTime: LocalTime,
        endTime: LocalTime,
        room: String?,
        teacher: String?
    ): Boolean {
        clearValidationError()
        val version = _activeVersion.value ?: return false

        // Conflict check
        val overlapping = detectConflictUseCase(version.id, day, startTime, endTime, id)
        if (overlapping.isNotEmpty()) {
            _conflicts.value = overlapping
            _validationState.value = ValidationResult.Invalid("Timing overlap conflict detected")
            return false
        }

        val schedule = Schedule(
            id = id,
            subjectId = subjectId,
            dayOfWeek = day,
            startTime = startTime,
            endTime = endTime,
            room = room?.trim()?.takeIf { it.isNotBlank() },
            teacherOverride = teacher?.trim()?.takeIf { it.isNotBlank() },
            versionId = version.id,
            updatedAt = System.currentTimeMillis()
        )

        val existing = getWeekScheduleUseCase(version.id)
        val validationResult = validator.validate(schedule, existing)
        if (validationResult is ValidationResult.Invalid) {
            _validationState.value = validationResult
            return false
        }

        return try {
            if (id == 0L) {
                addScheduleUseCase(schedule)
            } else {
                updateScheduleUseCase(schedule)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Saves a class slot that may span multiple weekdays.
     * One schedule record is persisted per selected day via
     * [SaveMultiDayScheduleUseCase]. Afterwards the semester history is checked
     * for classes with missing attendance and, if any are found, a backfill
     * prompt is surfaced through [pendingBackfillPrompt].
     */
    suspend fun saveScheduleMultiDay(
        id: Long = 0L,
        subjectId: Long,
        days: Set<WeekDay>,
        startTime: LocalTime,
        endTime: LocalTime,
        room: String?,
        teacher: String?
    ): Boolean {
        clearValidationError()
        val version = _activeVersion.value ?: return false
        val selectedDays = days.toSet()
        if (selectedDays.isEmpty()) {
            _validationState.value = ValidationResult.Invalid("Select at least one day of the week")
            return false
        }

        val allSchedules = getWeekScheduleUseCase(version.id)
        val groupMembers = if (id == 0L) {
            emptyList()
        } else {
            allSchedules.filter {
                it.id != id && it.subjectId == subjectId &&
                        it.startTime == startTime && it.endTime == endTime
            }
        }
        val excludedIds = groupMembers.map { it.id }.toSet() + id

        // Conflict check per day, ignoring the slot group being edited.
        for (day in selectedDays) {
            val overlapping = detectConflictUseCase(version.id, day, startTime, endTime, id)
                .filter { it.id !in excludedIds }
            if (overlapping.isNotEmpty()) {
                _conflicts.value = overlapping
                _validationState.value = ValidationResult.Invalid("Timing overlap conflict detected")
                return false
            }
        }

        // Validation per day, ignoring the slot group being edited.
        val existingExcludingGroup = allSchedules.filter { it.id !in excludedIds }
        for (day in selectedDays) {
            val schedule = Schedule(
                id = id,
                subjectId = subjectId,
                dayOfWeek = day,
                startTime = startTime,
                endTime = endTime,
                room = room?.trim()?.takeIf { it.isNotBlank() },
                teacherOverride = teacher?.trim()?.takeIf { it.isNotBlank() },
                versionId = version.id,
                updatedAt = System.currentTimeMillis()
            )
            val validationResult = validator.validate(schedule, existingExcludingGroup)
            if (validationResult is ValidationResult.Invalid) {
                _validationState.value = validationResult
                return false
            }
        }

        return try {
            saveMultiDayScheduleUseCase(
                versionId = version.id,
                anchorId = id,
                subjectId = subjectId,
                days = selectedDays,
                startTime = startTime,
                endTime = endTime,
                room = room,
                teacher = teacher
            )
            detectAndPromptBackfill(subjectId = subjectId, version = version)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun dismissBackfillPrompt() {
        _pendingBackfillPrompt.value = null
    }

    private suspend fun detectAndPromptBackfill(subjectId: Long, version: SemesterVersion) {
        val today = LocalDate.now()
        if (version.startDate.isAfter(today)) {
            _pendingBackfillPrompt.value = null
            return
        }

        val subjectSchedules = scheduleRepository.getSchedulesBySubject(subjectId)
            .filter { it.versionId == version.id }
        if (subjectSchedules.isEmpty()) {
            _pendingBackfillPrompt.value = null
            return
        }

        val attendance = attendanceRepository.observeAttendanceHistory().first()
        val missing = detectMissingAttendanceUseCase(
            schedules = subjectSchedules,
            attendance = attendance,
            from = version.startDate,
            to = today
        )

        _pendingBackfillPrompt.value = if (missing.isEmpty()) {
            null
        } else {
            val subjectName = subjectRepository.getSubjects()
                .find { it.id == subjectId }?.name ?: "Subject"
            BackfillPrompt(
                subjectId = subjectId,
                subjectName = subjectName,
                missingCount = missing.size
            )
        }
    }

    fun deleteSchedule(scheduleUiModel: ScheduleUiModel) {
        viewModelScope.launch {
            val version = _activeVersion.value ?: return@launch
            val domain = ScheduleMapper.uiToDomain(scheduleUiModel, version.id)
            deleteScheduleUseCase(domain)
        }
    }

    fun createTimetableVersion(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                val newId = semesterRepository.insertVersion(
                    SemesterVersion(name = name.trim(), isActive = false)
                )
                semesterRepository.switchActiveVersion(newId)
            }
        }
    }

    fun switchActiveVersion(versionId: Long) {
        viewModelScope.launch {
            switchTimetableVersionUseCase(versionId)
        }
    }

    fun deleteTimetableVersion(versionId: Long) {
        viewModelScope.launch {
            val active = _activeVersion.value
            if (active?.id == versionId) {
                // Cannot delete current active without switching first.
                // We'll auto switch to another version if available.
                val other = _versions.value.firstOrNull { it.id != versionId }
                if (other != null) {
                    semesterRepository.switchActiveVersion(other.id)
                    semesterRepository.deleteVersion(SemesterVersion(id = versionId, name = ""))
                }
            } else {
                semesterRepository.deleteVersion(SemesterVersion(id = versionId, name = ""))
            }
        }
    }

    fun renameTimetableVersion(versionId: Long, newName: String) {
        viewModelScope.launch {
            if (newName.isNotBlank()) {
                val existing = semesterRepository.getVersion(versionId)
                if (existing != null) {
                    semesterRepository.updateVersion(existing.copy(name = newName.trim()))
                }
            }
        }
    }

    suspend fun getScheduleById(id: Long): Schedule? {
        return scheduleRepository.getSchedule(id)
    }
}
