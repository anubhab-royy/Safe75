package com.attendance.tracker.feature.attendance

import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.model.SemesterVersion
import com.attendance.tracker.domain.model.Subject
import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.domain.usecase.attendance.*
import com.attendance.tracker.domain.validation.AttendanceValidator
import com.attendance.tracker.domain.validation.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ReactiveFakeAttendanceRepository : com.attendance.tracker.domain.repository.AttendanceRepository {
    private val _list = MutableStateFlow<List<Attendance>>(emptyList())

    override fun observeAttendanceForSubject(subjectId: Long): Flow<List<Attendance>> {
        return _list.map { it.filter { s -> s.subjectId == subjectId } }
    }

    override fun observeAttendanceForDate(date: LocalDate): Flow<List<Attendance>> {
        return _list.map { it.filter { s -> s.date == date } }
    }

    override fun observeTodayAttendance(date: LocalDate): Flow<List<Attendance>> {
        return _list.map { it.filter { s -> s.date == date } }
    }

    override fun observeAttendanceHistory(): Flow<List<Attendance>> {
        return _list.map { it.sortedByDescending { s -> s.date } }
    }

    override fun observeAttendanceById(id: Long): Flow<Attendance?> {
        return _list.map { it.find { s -> s.id == id } }
    }

    override suspend fun getAttendanceById(id: Long): Attendance? = _list.value.find { it.id == id }

    override suspend fun getAttendanceForDateSync(date: LocalDate): List<Attendance> = _list.value.filter { it.date == date }

    override suspend fun insertAttendance(attendance: Attendance): Long {
        val current = _list.value.toMutableList()
        val id = if (attendance.id == 0L) (current.size + 1).toLong() else attendance.id
        current.removeAll { it.id == id }
        current.add(attendance.copy(id = id))
        _list.value = current
        return id
    }

    override suspend fun updateAttendance(attendance: Attendance): Int {
        val current = _list.value.toMutableList()
        val idx = current.indexOfFirst { it.id == attendance.id }
        return if (idx != -1) {
            current[idx] = attendance
            _list.value = current
            1
        } else {
            0
        }
    }

    override suspend fun deleteAttendance(attendance: Attendance): Int {
        val current = _list.value.toMutableList()
        return if (current.removeIf { it.id == attendance.id }) {
            _list.value = current
            1
        } else {
            0
        }
    }

    override suspend fun checkDuplicateAttendance(subjectId: Long, scheduleId: Long, date: LocalDate): Boolean {
        return _list.value.any { it.subjectId == subjectId && it.scheduleId == scheduleId && it.date == date }
    }

    override fun countPresent(): Flow<Int> = _list.map { it.count { it.status == AttendanceStatus.PRESENT } }
    override fun countAbsent(): Flow<Int> = _list.map { it.count { it.status == AttendanceStatus.ABSENT } }
    override fun countCancelled(): Flow<Int> = _list.map { it.count { it.status == AttendanceStatus.CANCELLED } }
    override fun searchAttendance(query: String): Flow<List<Attendance>> = _list.map {
        it.filter { s -> s.remarks?.contains(query, ignoreCase = true) == true }
    }
}

class ReactiveFakeSubjectRepository : com.attendance.tracker.domain.repository.SubjectRepository {
    private val _list = MutableStateFlow<List<Subject>>(emptyList())
    override fun observeSubjects(): Flow<List<Subject>> = _list
    override suspend fun getSubjects(): List<Subject> = _list.value
    override suspend fun getSubjectById(id: Long): Subject? = _list.value.find { it.id == id }
    override suspend fun insertSubject(subject: Subject): Long {
        val current = _list.value.toMutableList()
        val id = if (subject.id == 0L) (current.size + 1).toLong() else subject.id
        current.add(subject.copy(id = id))
        _list.value = current
        return id
    }
    override suspend fun updateSubject(subject: Subject): Int = 0
    override suspend fun deleteSubject(subject: Subject): Int = 0
    override suspend fun searchSubjects(query: String): List<Subject> = _list.value.filter { it.name.contains(query, ignoreCase = true) }
    override suspend fun countSubjects(): Int = _list.value.size
}

class ReactiveFakeScheduleRepository : com.attendance.tracker.domain.repository.ScheduleRepository {
    private val _list = MutableStateFlow<List<Schedule>>(emptyList())
    override fun observeSchedulesForVersion(versionId: Long): Flow<List<Schedule>> = _list.map { it.filter { s -> s.versionId == versionId } }
    override fun observeSchedule(id: Long): Flow<Schedule?> = _list.map { it.find { s -> s.id == id } }
    override suspend fun getSchedule(id: Long): Schedule? = _list.value.find { it.id == id }
    override suspend fun getSchedulesForVersion(versionId: Long): List<Schedule> = _list.value.filter { it.versionId == versionId }
    override fun observeTodaySchedules(versionId: Long, day: WeekDay): Flow<List<Schedule>> = _list.map { it.filter { s -> s.versionId == versionId && s.dayOfWeek == day } }
    override suspend fun getSchedulesBySubject(subjectId: Long): List<Schedule> = _list.value.filter { it.subjectId == subjectId }
    override suspend fun getSchedulesByDay(versionId: Long, day: WeekDay): List<Schedule> = _list.value.filter { it.versionId == versionId && it.dayOfWeek == day }
    override suspend fun searchSchedules(versionId: Long, query: String): List<Schedule> = emptyList()
    override suspend fun checkConflicts(versionId: Long, day: WeekDay, start: LocalTime, end: LocalTime): List<Schedule> = emptyList()
    override suspend fun insertSchedule(schedule: Schedule): Long {
        val current = _list.value.toMutableList()
        val id = if (schedule.id == 0L) (current.size + 1).toLong() else schedule.id
        current.add(schedule.copy(id = id))
        _list.value = current
        return id
    }
    override suspend fun updateSchedule(schedule: Schedule): Int = 0
    override suspend fun deleteSchedule(schedule: Schedule): Int = 0
}

class ReactiveFakeSemesterRepository : com.attendance.tracker.domain.repository.SemesterRepository {
    private val _list = MutableStateFlow<List<SemesterVersion>>(emptyList())
    override fun observeVersions(): Flow<List<SemesterVersion>> = _list
    override fun observeActiveVersion(): Flow<SemesterVersion?> = _list.map { it.find { it.isActive } }
    override suspend fun getVersions(): List<SemesterVersion> = _list.value
    override suspend fun getVersion(id: Long): SemesterVersion? = _list.value.find { it.id == id }
    override suspend fun getActiveVersion(): SemesterVersion? = _list.value.find { it.isActive }
    override suspend fun insertVersion(version: SemesterVersion): Long {
        val current = _list.value.toMutableList()
        current.add(version)
        _list.value = current
        return version.id
    }
    override suspend fun updateVersion(version: SemesterVersion): Int = 0
    override suspend fun deleteVersion(version: SemesterVersion): Int = 0
    override suspend fun switchActiveVersion(versionId: Long) {
        val current = _list.value.map { ver ->
            ver.copy(isActive = ver.id == versionId)
        }
        _list.value = current
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var attendanceRepo: ReactiveFakeAttendanceRepository
    private lateinit var subjectRepo: ReactiveFakeSubjectRepository
    private lateinit var scheduleRepo: ReactiveFakeScheduleRepository
    private lateinit var semesterRepo: ReactiveFakeSemesterRepository

    private lateinit var viewModel: AttendanceViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        attendanceRepo = ReactiveFakeAttendanceRepository()
        subjectRepo = ReactiveFakeSubjectRepository()
        scheduleRepo = ReactiveFakeScheduleRepository()
        semesterRepo = ReactiveFakeSemesterRepository()

        val validator = AttendanceValidator()
        val markUseCase = MarkAttendanceUseCase(attendanceRepo, validator)
        val updateUseCase = UpdateAttendanceUseCase(attendanceRepo, validator)
        val deleteUseCase = DeleteAttendanceUseCase(attendanceRepo)
        val getHistoryUseCase = GetAttendanceHistoryUseCase(attendanceRepo)
        val getTodayUseCase = GetTodayAttendanceUseCase(attendanceRepo)
        val calculateStatsUseCase = CalculateAttendanceStatisticsUseCase()

        viewModel = AttendanceViewModel(
            markAttendanceUseCase = markUseCase,
            updateAttendanceUseCase = updateUseCase,
            deleteAttendanceUseCase = deleteUseCase,
            getAttendanceHistoryUseCase = getHistoryUseCase,
            getTodayAttendanceUseCase = getTodayUseCase,
            calculateAttendanceStatisticsUseCase = calculateStatsUseCase,
            subjectRepository = subjectRepo,
            scheduleRepository = scheduleRepo,
            semesterRepository = semesterRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testMarkAttendance_savesSuccessfully() = runTest {
        advanceUntilIdle()

        // Populate a subject and version
        val subjId = subjectRepo.insertSubject(Subject(id = 1L, name = "Software Eng", requiredAttendancePercentage = 75, personalAttendanceGoal = 85))
        semesterRepo.insertVersion(SemesterVersion(id = 1L, name = "Version 1", isActive = true))
        val day = when (LocalDate.now().dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> WeekDay.Monday
            java.time.DayOfWeek.TUESDAY -> WeekDay.Tuesday
            java.time.DayOfWeek.WEDNESDAY -> WeekDay.Wednesday
            java.time.DayOfWeek.THURSDAY -> WeekDay.Thursday
            java.time.DayOfWeek.FRIDAY -> WeekDay.Friday
            java.time.DayOfWeek.SATURDAY -> WeekDay.Saturday
            java.time.DayOfWeek.SUNDAY -> WeekDay.Sunday
        }
        val schedId = scheduleRepo.insertSchedule(Schedule(id = 10L, subjectId = subjId, dayOfWeek = day, startTime = LocalTime.NOON, endTime = LocalTime.NOON.plusHours(1), versionId = 1L))
        advanceUntilIdle()

        // Perform mark
        val success = viewModel.markAttendance(subjectId = subjId, scheduleId = schedId, status = AttendanceStatus.PRESENT)
        advanceUntilIdle()

        assertTrue(success)
        assertEquals(1, attendanceRepo.getAttendanceForDateSync(LocalDate.now()).size)
    }

    @Test
    fun testDuplicateLogs_failValidation() = runTest {
        advanceUntilIdle()
        val subjId = subjectRepo.insertSubject(Subject(id = 2L, name = "Maths"))
        val day = when (LocalDate.now().dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> WeekDay.Monday
            java.time.DayOfWeek.TUESDAY -> WeekDay.Tuesday
            java.time.DayOfWeek.WEDNESDAY -> WeekDay.Wednesday
            java.time.DayOfWeek.THURSDAY -> WeekDay.Thursday
            java.time.DayOfWeek.FRIDAY -> WeekDay.Friday
            java.time.DayOfWeek.SATURDAY -> WeekDay.Saturday
            java.time.DayOfWeek.SUNDAY -> WeekDay.Sunday
        }
        val schedId = scheduleRepo.insertSchedule(Schedule(id = 11L, subjectId = subjId, dayOfWeek = day, startTime = LocalTime.NOON, endTime = LocalTime.NOON.plusHours(1), versionId = 1L))
        advanceUntilIdle()

        // First log
        val success1 = viewModel.markAttendance(subjectId = subjId, scheduleId = schedId, status = AttendanceStatus.PRESENT)
        assertTrue(success1)

        // Second log on same subject/schedule/date
        val success2 = viewModel.markAttendance(subjectId = subjId, scheduleId = schedId, status = AttendanceStatus.ABSENT)
        advanceUntilIdle()

        assertTrue(!success2)
        assertTrue(viewModel.validationState.value is ValidationResult.Invalid)
        assertEquals("Attendance has already been marked for this class slot on this date", (viewModel.validationState.value as ValidationResult.Invalid).reason)
    }
}
