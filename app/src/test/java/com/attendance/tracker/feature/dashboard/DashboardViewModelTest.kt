package com.attendance.tracker.feature.dashboard

import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.model.SemesterVersion
import com.attendance.tracker.domain.model.Subject
import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.domain.usecase.attendance.CalculateAttendanceStatisticsUseCase
import com.attendance.tracker.domain.usecase.attendance.GetTodayAttendanceUseCase
import com.attendance.tracker.domain.usecase.planner.AttendanceSimulatorUseCase
import com.attendance.tracker.domain.usecase.planner.LeavePlannerUseCase
import com.attendance.tracker.domain.usecase.planner.LocalFakeAttendanceRepository
import com.attendance.tracker.domain.usecase.planner.LocalFakeScheduleRepository
import com.attendance.tracker.domain.usecase.planner.LocalFakeSubjectRepository
import com.attendance.tracker.domain.usecase.attendance.MarkAttendanceUseCase
import com.attendance.tracker.domain.usecase.attendance.UpdateAttendanceUseCase
import com.attendance.tracker.domain.usecase.attendance.DeleteAttendanceUseCase
import com.attendance.tracker.domain.validation.AttendanceValidator
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class LocalFakeSemesterRepository : com.attendance.tracker.domain.repository.SemesterRepository {
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
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var attendanceRepo: LocalFakeAttendanceRepository
    private lateinit var subjectRepo: LocalFakeSubjectRepository
    private lateinit var scheduleRepo: LocalFakeScheduleRepository
    private lateinit var semesterRepo: LocalFakeSemesterRepository

    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        attendanceRepo = LocalFakeAttendanceRepository()
        subjectRepo = LocalFakeSubjectRepository()
        scheduleRepo = LocalFakeScheduleRepository()
        semesterRepo = LocalFakeSemesterRepository()

        val calculateStats = CalculateAttendanceStatisticsUseCase()
        val getTodayAttendance = GetTodayAttendanceUseCase(attendanceRepo)
        val simulator = AttendanceSimulatorUseCase()
        val leavePlanner = LeavePlannerUseCase(subjectRepo, scheduleRepo, attendanceRepo)
        val validator = AttendanceValidator()
        val markAttendance = MarkAttendanceUseCase(attendanceRepo, validator)
        val updateAttendance = UpdateAttendanceUseCase(attendanceRepo, validator)
        val deleteAttendance = DeleteAttendanceUseCase(attendanceRepo)

        viewModel = DashboardViewModel(
            attendanceRepository = attendanceRepo,
            subjectRepository = subjectRepo,
            scheduleRepository = scheduleRepo,
            semesterRepository = semesterRepo,
            calculateStatisticsUseCase = calculateStats,
            getTodayAttendanceUseCase = getTodayAttendance,
            attendanceSimulatorUseCase = simulator,
            leavePlannerUseCase = leavePlanner,
            markAttendanceUseCase = markAttendance,
            updateAttendanceUseCase = updateAttendance,
            deleteAttendanceUseCase = deleteAttendance
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialFilterScope_isOverall() = runTest {
        assertEquals(DashboardFilter.Overall, viewModel.filter.value)
    }

    @Test
    fun testFilterScopeChange_filtersLogsCorrectly() = runTest {
        advanceUntilIdle()

        // Log one today record, and one yesterday record
        val subjId = subjectRepo.insertSubject(Subject(id = 1L, name = "SE"))
        attendanceRepo.insertAttendance(Attendance(id = 1L, subjectId = subjId, scheduleId = 10L, date = LocalDate.now(), status = AttendanceStatus.PRESENT))
        attendanceRepo.insertAttendance(Attendance(id = 2L, subjectId = subjId, scheduleId = 10L, date = LocalDate.now().minusDays(5), status = AttendanceStatus.ABSENT))
        advanceUntilIdle()

        // Default: Overall (both records)
        assertEquals(2, viewModel.filteredHistory.value.size)

        // Switch filter: Today
        viewModel.onFilterScopeChange(DashboardFilter.Today)
        advanceUntilIdle()
        assertEquals(1, viewModel.filteredHistory.value.size)
        assertEquals(LocalDate.now(), viewModel.filteredHistory.value.first().date)
    }

    @Test
    fun testSimulatorFlow_updatesProjStats() = runTest {
        advanceUntilIdle()
        viewModel.onSimPresentChange(5)
        viewModel.onSimAbsentChange(1)
        advanceUntilIdle()

        val simResult = viewModel.simulationResult.value
        assertNotNull(simResult)
        assertEquals(83.3, simResult!!.simulatedPercentage, 0.1)
    }

    @Test
    fun testTodayClassesAndToggling() = runTest {
        // 1. Setup active semester, subjects and schedules
        semesterRepo.insertVersion(SemesterVersion(id = 5L, name = "V1", isActive = true))
        val subjId = subjectRepo.insertSubject(Subject(id = 10L, name = "SE"))
        
        // Find current day enum
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

        val schedId = scheduleRepo.insertSchedule(
            Schedule(
                id = 20L,
                subjectId = subjId,
                dayOfWeek = dayEnum,
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0),
                versionId = 5L
            )
        )
        advanceUntilIdle()

        // 2. Verify todayClasses contains the scheduled item and is initially unmarked
        val classes = viewModel.todayClasses.value
        assertEquals(1, classes.size)
        val item = classes.first()
        assertEquals(20L, item.scheduleId)
        assertEquals(10L, item.subjectId)
        assertEquals("SE", item.subjectName)
        assertEquals(null, item.attendance)

        // 3. Mark class as PRESENT
        viewModel.onAttendanceStatusClick(item, AttendanceStatus.PRESENT)
        advanceUntilIdle()
        val markedItem = viewModel.todayClasses.value.first()
        assertNotNull(markedItem.attendance)
        assertEquals(AttendanceStatus.PRESENT, markedItem.attendance!!.status)

        // 4. Toggle Present to ABSENT
        viewModel.onAttendanceStatusClick(markedItem, AttendanceStatus.ABSENT)
        advanceUntilIdle()
        val changedItem = viewModel.todayClasses.value.first()
        assertNotNull(changedItem.attendance)
        assertEquals(AttendanceStatus.ABSENT, changedItem.attendance!!.status)

        // 5. Click ABSENT again to clear selection (delete attendance)
        viewModel.onAttendanceStatusClick(changedItem, AttendanceStatus.ABSENT)
        advanceUntilIdle()
        val clearedItem = viewModel.todayClasses.value.first()
        assertEquals(null, clearedItem.attendance)
    }
}
