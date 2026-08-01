package com.attendance.tracker.feature.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.tracker.core.common.AppError
import com.attendance.tracker.core.common.UiState
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.data.mapper.ScheduleMapper
import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.domain.model.SemesterVersion
import com.attendance.tracker.domain.repository.ScheduleRepository
import com.attendance.tracker.domain.repository.SemesterRepository
import com.attendance.tracker.domain.repository.SubjectRepository
import com.attendance.tracker.domain.usecase.schedule.AddScheduleUseCase
import com.attendance.tracker.domain.usecase.schedule.DeleteScheduleUseCase
import com.attendance.tracker.domain.usecase.schedule.DetectConflictUseCase
import com.attendance.tracker.domain.usecase.schedule.GetWeekScheduleUseCase
import com.attendance.tracker.domain.usecase.schedule.ObserveScheduleUseCase
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

/**
 * ViewModel managing the active weekly schedules, conflicts, and timetable version configurations.
 */
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val observeScheduleUseCase: ObserveScheduleUseCase,
    private val getWeekScheduleUseCase: GetWeekScheduleUseCase,
    private val addScheduleUseCase: AddScheduleUseCase,
    private val updateScheduleUseCase: UpdateScheduleUseCase,
    private val deleteScheduleUseCase: DeleteScheduleUseCase,
    private val detectConflictUseCase: DetectConflictUseCase,
    private val switchTimetableVersionUseCase: SwitchTimetableVersionUseCase,
    private val semesterRepository: SemesterRepository,
    private val subjectRepository: SubjectRepository,
    private val scheduleRepository: ScheduleRepository,
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
    }

    private fun initVersions() {
        viewModelScope.launch {
            semesterRepository.observeActiveVersion().collect { active ->
                if (active != null) {
                    _activeVersion.value = active
                    observeSchedules(active.id)
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

    private fun observeSchedules(versionId: Long) {
        viewModelScope.launch {
            combine(
                observeScheduleUseCase(versionId),
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
            }.catch {
                _schedules.value = emptyList()
            }.collect { list ->
                _schedules.value = list
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
