package com.attendance.tracker.feature.backfill

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.domain.model.Subject
import com.attendance.tracker.domain.repository.AttendanceRepository
import com.attendance.tracker.domain.repository.ScheduleRepository
import com.attendance.tracker.domain.repository.SemesterRepository
import com.attendance.tracker.domain.repository.SubjectRepository
import com.attendance.tracker.domain.usecase.attendance.DetectMissingAttendanceUseCase
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

/**
 * UI-layer representation of a single historical class missing attendance,
 * enriched with the subject display details for the backfill wizard.
 */
data class BackfillWizardItem(
    val scheduleId: Long,
    val subjectId: Long,
    val subjectName: String,
    val subjectColor: Int,
    val date: LocalDate,
    val dayOfWeek: WeekDay,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val room: String?
)

/**
 * ViewModel powering the Backfill Attendance Wizard.
 *
 * Recomputes every class occurrence expected between the active semester start
 * date and today, filters out occurrences that already have an attendance
 * record, and exposes the remaining missing entries. Marking entries persists
 * attendance immediately so the list reacts to database changes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BackfillWizardViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val semesterRepository: SemesterRepository,
    private val attendanceRepository: AttendanceRepository,
    private val subjectRepository: SubjectRepository,
    private val detectMissingAttendanceUseCase: DetectMissingAttendanceUseCase
) : ViewModel() {

    private val _subjectFilter = MutableStateFlow(-1L)
    private val _schedules = MutableStateFlow<List<Schedule>>(emptyList())
    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())

    init {
        viewModelScope.launch {
            semesterRepository.observeActiveVersion()
                .flatMapLatest { active ->
                    if (active != null) {
                        scheduleRepository.observeSchedulesForVersion(active.id)
                    } else {
                        flowOf(emptyList())
                    }
                }
                .collect { list ->
                    _schedules.value = list
                }
        }

        viewModelScope.launch {
            subjectRepository.observeSubjects().collect { list ->
                _subjects.value = list
            }
        }
    }

    /** Restricts the wizard to one subject; pass -1L to show every subject. */
    fun setSubjectFilter(subjectId: Long) {
        _subjectFilter.value = subjectId
    }

    val missingItems: StateFlow<List<BackfillWizardItem>> = combine(
        semesterRepository.observeActiveVersion(),
        _schedules,
        attendanceRepository.observeAttendanceHistory(),
        _subjects,
        _subjectFilter
    ) { activeVer, schedules, attendance, subjects, filter ->
        if (activeVer == null) {
            emptyList()
        } else {
            val today = LocalDate.now()
            val applicable = schedules.filter { it.versionId == activeVer.id }
            detectMissingAttendanceUseCase(
                schedules = applicable,
                attendance = attendance,
                from = activeVer.startDate,
                to = today
            )
                .filter { filter <= 0L || it.subjectId == filter }
                .map { item ->
                    val subject = subjects.find { it.id == item.subjectId }
                    BackfillWizardItem(
                        scheduleId = item.scheduleId,
                        subjectId = item.subjectId,
                        subjectName = subject?.name ?: "Unknown Subject",
                        subjectColor = subject?.color ?: 0xFF9E9E9E.toInt(),
                        date = item.date,
                        dayOfWeek = item.dayOfWeek,
                        startTime = item.startTime,
                        endTime = item.endTime,
                        room = item.room
                    )
                }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    /**
     * Marks a single past class. Tapping the same status again removes the
     * record, which returns the class to the missing list.
     */
    fun markAttendance(item: BackfillWizardItem, status: AttendanceStatus) {
        viewModelScope.launch {
            val recorded = attendanceRepository.observeAttendanceHistory().first()
                .associateBy { it.scheduleId to it.date }
            val existing = recorded[item.scheduleId to item.date]
            when {
                existing == null -> attendanceRepository.insertAttendance(
                    Attendance(
                        subjectId = item.subjectId,
                        scheduleId = item.scheduleId,
                        date = item.date,
                        status = status
                    )
                )
                existing.status == status -> attendanceRepository.deleteAttendance(existing)
                else -> attendanceRepository.updateAttendance(
                    existing.copy(status = status, updatedAt = System.currentTimeMillis())
                )
            }
        }
    }

    /** Applies a status to every currently-missing entry in one shot. */
    fun markAll(status: AttendanceStatus) {
        val items = missingItems.value
        if (items.isEmpty()) return
        viewModelScope.launch {
            val recorded = attendanceRepository.observeAttendanceHistory().first()
                .associateBy { it.scheduleId to it.date }
            items.forEach { item ->
                val existing = recorded[item.scheduleId to item.date]
                when {
                    existing == null -> attendanceRepository.insertAttendance(
                        Attendance(
                            subjectId = item.subjectId,
                            scheduleId = item.scheduleId,
                            date = item.date,
                            status = status
                        )
                    )
                    existing.status != status -> attendanceRepository.updateAttendance(
                        existing.copy(status = status, updatedAt = System.currentTimeMillis())
                    )
                }
            }
        }
    }
}
