package com.attendance.tracker.feature.ocr.review

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.model.Subject
import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.domain.model.SemesterVersion
import com.attendance.tracker.domain.repository.SubjectRepository
import com.attendance.tracker.domain.repository.ScheduleRepository
import com.attendance.tracker.domain.repository.SemesterRepository
import com.attendance.tracker.domain.repository.AttendanceRepository
import com.attendance.tracker.feature.ocr.model.OcrTimetableRow
import com.attendance.tracker.feature.ocr.model.OcrAttendanceRow
import com.attendance.tracker.feature.ocr.repository.OcrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * ViewModel managing the raw recognized items review and persistence workflow.
 */
@HiltViewModel
class OcrReviewViewModel @Inject constructor(
    private val ocrRepository: OcrRepository,
    private val subjectRepository: SubjectRepository,
    private val scheduleRepository: ScheduleRepository,
    private val semesterRepository: SemesterRepository,
    private val attendanceRepository: AttendanceRepository
) : ViewModel() {

    private val _timetableRows = MutableStateFlow<List<OcrTimetableRow>>(emptyList())
    val timetableRows = _timetableRows.asStateFlow()

    private val _attendanceRows = MutableStateFlow<List<OcrAttendanceRow>>(emptyList())
    val attendanceRows = _attendanceRows.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects = _subjects.asStateFlow()

    init {
        viewModelScope.launch {
            subjectRepository.observeSubjects().collect {
                _subjects.value = it
            }
        }
    }

    /**
     * Executes ML Kit timetable text scanning.
     */
    fun scanTimetable(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            ocrRepository.importTimetable(context, uri)
                .onSuccess { _timetableRows.value = it }
                .onFailure { _error.value = it.message ?: "Failed to recognize text" }
            _isLoading.value = false
        }
    }

    /**
     * Executes ML Kit ERP attendance text scanning.
     */
    fun scanAttendance(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            ocrRepository.importAttendance(context, uri)
                .onSuccess { parsedRows ->
                    val existing = subjectRepository.getSubjects()
                    val mapped = parsedRows.map { row ->
                        val matched = existing.find { it.name.equals(row.subjectName.value, ignoreCase = true) }
                        row.copy(matchedSubjectId = matched?.id)
                    }
                    _attendanceRows.value = mapped
                }
                .onFailure { _error.value = it.message ?: "Failed to recognize text" }
            _isLoading.value = false
        }
    }

    fun updateTimetableRow(updated: OcrTimetableRow) {
        val current = _timetableRows.value.toMutableList()
        val idx = current.indexOfFirst { it.id == updated.id }
        if (idx != -1) {
            current[idx] = updated
            _timetableRows.value = current
        }
    }

    fun deleteTimetableRow(id: String) {
        val current = _timetableRows.value.toMutableList()
        current.removeAll { it.id == id }
        _timetableRows.value = current
    }

    fun updateAttendanceRow(updated: OcrAttendanceRow) {
        val current = _attendanceRows.value.toMutableList()
        val idx = current.indexOfFirst { it.id == updated.id }
        if (idx != -1) {
            current[idx] = updated
            _attendanceRows.value = current
        }
    }

    fun deleteAttendanceRow(id: String) {
        val current = _attendanceRows.value.toMutableList()
        current.removeAll { it.id == id }
        _attendanceRows.value = current
    }

    /**
     * Persists parsed timetable rows to the database.
     */
    suspend fun saveTimetable(): Boolean {
        val activeVersion = semesterRepository.getActiveVersion() ?: return false

        _timetableRows.value.forEach { row ->
            val subjectName = row.subjectName.value
            val existingSubjects = subjectRepository.getSubjects()
            var subjectId = existingSubjects.find { it.name.equals(subjectName, ignoreCase = true) }?.id

            if (subjectId == null) {
                val newSub = Subject(
                    name = subjectName,
                    requiredAttendancePercentage = 75,
                    personalAttendanceGoal = 85,
                    color = 0xFF9E9E9E.toInt(),
                    facultyName = row.faculty.value
                )
                subjectId = subjectRepository.insertSubject(newSub)
            }

            val dayEnum = try {
                WeekDay.valueOf(row.dayOfWeek.value)
            } catch (e: Exception) {
                WeekDay.Monday
            }

            val start = try {
                LocalTime.parse(row.startTime.value)
            } catch (e: Exception) {
                LocalTime.of(9, 0)
            }

            val end = try {
                LocalTime.parse(row.endTime.value)
            } catch (e: Exception) {
                LocalTime.of(10, 0)
            }

            val schedule = Schedule(
                subjectId = subjectId,
                dayOfWeek = dayEnum,
                startTime = start,
                endTime = end,
                room = row.room.value,
                teacherOverride = row.faculty.value,
                versionId = activeVersion.id
            )
            scheduleRepository.insertSchedule(schedule)
        }
        _timetableRows.value = emptyList()
        return true
    }

    /**
     * Persists parsed attendance logs.
     */
    suspend fun saveAttendance(mappings: Map<String, Long>): Boolean {
        _attendanceRows.value.forEach { row ->
            val subjectId = row.matchedSubjectId ?: mappings[row.id] ?: return@forEach

            val total = row.totalClasses.value
            val present = row.presentCount.value

            for (i in 1..total) {
                val status = if (i <= present) AttendanceStatus.PRESENT else AttendanceStatus.ABSENT
                val date = LocalDate.now().minusDays(i.toLong())

                val log = Attendance(
                    subjectId = subjectId,
                    scheduleId = 0L,
                    date = date,
                    status = status,
                    remarks = "Imported via OCR"
                )
                attendanceRepository.insertAttendance(log)
            }
        }
        _attendanceRows.value = emptyList()
        return true
    }
}
