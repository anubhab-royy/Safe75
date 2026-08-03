package com.attendance.tracker.feature.subject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.tracker.core.common.AppError
import com.attendance.tracker.core.common.UiState
import com.attendance.tracker.data.mapper.SubjectMapper
import com.attendance.tracker.domain.model.Subject
import com.attendance.tracker.domain.usecase.subject.AddSubjectUseCase
import com.attendance.tracker.domain.usecase.subject.DeleteSubjectUseCase
import com.attendance.tracker.domain.usecase.subject.GetSubjectUseCase
import com.attendance.tracker.domain.usecase.subject.GetSubjectsUseCase
import com.attendance.tracker.domain.usecase.subject.ObserveSubjectsUseCase
import com.attendance.tracker.domain.usecase.subject.UpdateSubjectUseCase
import com.attendance.tracker.domain.validation.SubjectValidator
import com.attendance.tracker.domain.validation.ValidationResult
import com.attendance.tracker.feature.subject.model.SubjectUiModel
import com.attendance.tracker.feature.subject.model.SubjectWithStats
import com.attendance.tracker.domain.repository.AttendanceRepository
import com.attendance.tracker.domain.repository.SettingsRepository
import com.attendance.tracker.domain.usecase.attendance.CalculateAttendanceStatisticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sort options for organizing the subject list.
 */
enum class SubjectSortOption {
    ALPHABETICAL,
    RECENTLY_ADDED,
    ATTENDANCE_GOAL
}

/**
 * ViewModel for coordinating Subject Management actions and state.
 */
@HiltViewModel
class SubjectViewModel @Inject constructor(
    private val observeSubjectsUseCase: ObserveSubjectsUseCase,
    private val addSubjectUseCase: AddSubjectUseCase,
    private val updateSubjectUseCase: UpdateSubjectUseCase,
    private val deleteSubjectUseCase: DeleteSubjectUseCase,
    private val getSubjectUseCase: GetSubjectUseCase,
    private val getSubjectsUseCase: GetSubjectsUseCase,
    private val validator: SubjectValidator,
    private val attendanceRepository: AttendanceRepository,
    private val settingsRepository: SettingsRepository,
    private val calculateStatisticsUseCase: CalculateAttendanceStatisticsUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SubjectSortOption.ALPHABETICAL)
    val sortOption: StateFlow<SubjectSortOption> = _sortOption.asStateFlow()

    private val _subjectsState = MutableStateFlow<UiState<List<SubjectWithStats>>>(UiState.Loading)
    val subjectsState: StateFlow<UiState<List<SubjectWithStats>>> = _subjectsState.asStateFlow()

    private val _validationState = MutableStateFlow<ValidationResult>(ValidationResult.Valid)
    val validationState: StateFlow<ValidationResult> = _validationState.asStateFlow()

    init {
        loadSubjects()
    }

    private fun loadSubjects() {
        viewModelScope.launch {
            _subjectsState.value = UiState.Loading
            combine(
                observeSubjectsUseCase(),
                attendanceRepository.observeAttendanceHistory(),
                settingsRepository.getAttendanceTarget(),
                _searchQuery,
                _sortOption
            ) { rawSubjects, history, target, query, sort ->
                val filtered = if (query.isBlank()) {
                    rawSubjects
                } else {
                    rawSubjects.filter {
                        it.name.contains(query, ignoreCase = true) ||
                                (it.facultyName?.contains(query, ignoreCase = true) == true)
                    }
                }

                val sorted = when (sort) {
                    SubjectSortOption.ALPHABETICAL -> filtered.sortedBy { it.name.lowercase() }
                    SubjectSortOption.RECENTLY_ADDED -> filtered.sortedByDescending { it.createdAt }
                    SubjectSortOption.ATTENDANCE_GOAL -> filtered.sortedByDescending { it.personalAttendanceGoal }
                }

                sorted.map { subject ->
                    val subjectHistory = history.filter { it.subjectId == subject.id }
                    val stats = calculateStatisticsUseCase(
                        subjectHistory,
                        subject.requiredAttendancePercentage,
                        subject.personalAttendanceGoal
                    )
                    val status = when {
                        stats.attendancePercentage < subject.personalAttendanceGoal -> "CRITICAL"
                        stats.remainingSafeClasses == 0 -> "WARNING"
                        else -> "GOOD"
                    }
                    SubjectWithStats(
                        id = subject.id,
                        name = subject.name,
                        faculty = subject.facultyName,
                        requiredAttendance = subject.requiredAttendancePercentage,
                        attendanceGoal = subject.personalAttendanceGoal,
                        color = subject.color,
                        presentCount = stats.presentCount,
                        totalClasses = stats.totalClasses,
                        percentage = stats.attendancePercentage,
                        safeMissCount = stats.remainingSafeClasses,
                        classesNeeded = stats.classesNeededToReachGoal,
                        safetyStatus = status,
                        trend = "●"
                    )
                }
            }
                .catch { e ->
                    _subjectsState.value = UiState.Error(
                        message = e.localizedMessage ?: "Unknown Database Error",
                        throwable = AppError.DatabaseError("Failed to fetch subjects", e)
                    )
                }
                .collect { list ->
                    if (list.isEmpty() && _searchQuery.value.isBlank()) {
                        _subjectsState.value = UiState.Empty
                    } else {
                        _subjectsState.value = UiState.Success(list)
                    }
                }
        }
    }

    /**
     * Updates the search query filter.
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /**
     * Updates the sorting criterion.
     */
    fun onSortOptionChange(option: SubjectSortOption) {
        _sortOption.value = option
    }

    /**
     * Resets the validation error feedback state.
     */
    fun clearValidationError() {
        _validationState.value = ValidationResult.Valid
    }

    /**
     * Validates and saves (inserts or updates) a study subject.
     * Returns true if saved successfully, false on validation or db error.
     */
    suspend fun saveSubject(
        id: Long = 0L,
        name: String,
        faculty: String?,
        color: Int,
        required: Int,
        goal: Int
    ): Boolean {
        _validationState.value = ValidationResult.Valid
        
        val existing = try {
            getSubjectsUseCase()
        } catch (e: Exception) {
            emptyList()
        }

        val subject = Subject(
            id = id,
            name = name.trim(),
            facultyName = faculty?.trim()?.takeIf { it.isNotBlank() },
            color = color,
            requiredAttendancePercentage = required,
            personalAttendanceGoal = goal,
            updatedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis() // Will override with original if updating below
        )

        val validationResult = validator.validate(subject, existing)
        if (validationResult is ValidationResult.Invalid) {
            _validationState.value = validationResult
            return false
        }

        return try {
            if (id == 0L) {
                addSubjectUseCase(subject)
            } else {
                val original = getSubjectUseCase(id)
                val subjectWithOriginalTime = if (original != null) {
                    subject.copy(createdAt = original.createdAt)
                } else {
                    subject
                }
                updateSubjectUseCase(subjectWithOriginalTime)
            }
            true
        } catch (e: Exception) {
            _subjectsState.value = UiState.Error(
                message = "Failed to save subject: ${e.localizedMessage}",
                throwable = AppError.DatabaseError("Failed to save subject", e)
            )
            false
        }
    }

    /**
     * Deletes a subject by its unique ID.
     */
    fun deleteSubject(id: Long) {
        viewModelScope.launch {
            try {
                val subject = getSubjectUseCase(id)
                if (subject != null) {
                    deleteSubjectUseCase(subject)
                }
            } catch (e: Exception) {
                _subjectsState.value = UiState.Error(
                    message = "Failed to delete subject: ${e.localizedMessage}",
                    throwable = AppError.DatabaseError("Failed to delete subject", e)
                )
            }
        }
    }

    /**
     * Helper to load details for editing a subject.
     */
    suspend fun getSubjectById(id: Long): SubjectUiModel? {
        return try {
            getSubjectUseCase(id)?.let { SubjectMapper.domainToUi(it) }
        } catch (e: Exception) {
            null
        }
    }
}
