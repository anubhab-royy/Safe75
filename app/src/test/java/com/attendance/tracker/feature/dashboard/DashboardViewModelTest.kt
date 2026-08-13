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

class FakeSettingsRepository : com.attendance.tracker.domain.repository.SettingsRepository {
    private val _theme = MutableStateFlow("SYSTEM")
    private val _notifications = MutableStateFlow(true)
    private val _lastBackup = MutableStateFlow(0L)
    private val _target = MutableStateFlow(com.attendance.tracker.core.model.AttendanceTarget(75.0, 75.0))
    private val _morning = MutableStateFlow(true)
    private val _attendance = MutableStateFlow(true)
    private val _missed = MutableStateFlow(true)

    override fun getThemeMode(): Flow<String> = _theme
    override suspend fun setThemeMode(themeMode: String) { _theme.value = themeMode }
    override fun isNotificationsEnabled(): Flow<Boolean> = _notifications
    override suspend fun setNotificationsEnabled(enabled: Boolean) { _notifications.value = enabled }
    override fun getLastBackupTimestamp(): Flow<Long> = _lastBackup
    override suspend fun setLastBackupTimestamp(timestamp: Long) { _lastBackup.value = timestamp }
    override fun getAttendanceTarget(): Flow<com.attendance.tracker.core.model.AttendanceTarget> = _target
    override suspend fun updateAttendanceTarget(target: com.attendance.tracker.core.model.AttendanceTarget) { _target.value = target }
    override fun isMorningReminderEnabled(): Flow<Boolean> = _morning
    override suspend fun setMorningReminderEnabled(enabled: Boolean) { _morning.value = enabled }
    override fun isAttendanceReminderEnabled(): Flow<Boolean> = _attendance
    override suspend fun setAttendanceReminderEnabled(enabled: Boolean) { _attendance.value = enabled }
    override fun isMissedReminderEnabled(): Flow<Boolean> = _missed
    override suspend fun setMissedReminderEnabled(enabled: Boolean) { _missed.value = enabled }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var attendanceRepo: LocalFakeAttendanceRepository
    private lateinit var subjectRepo: LocalFakeSubjectRepository
    private lateinit var scheduleRepo: LocalFakeScheduleRepository
    private lateinit var semesterRepo: LocalFakeSemesterRepository
    private lateinit var settingsRepo: FakeSettingsRepository

    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        attendanceRepo = LocalFakeAttendanceRepository()
        subjectRepo = LocalFakeSubjectRepository()
        scheduleRepo = LocalFakeScheduleRepository()
        semesterRepo = LocalFakeSemesterRepository()
        settingsRepo = FakeSettingsRepository()

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
            deleteAttendanceUseCase = deleteAttendance,
            settingsRepository = settingsRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testAttendanceGoalChange_recalculatesAnalytics() = runTest {
        advanceUntilIdle()
        // Default goal: 75.0%
        assertEquals(75.0, viewModel.attendanceGoal.value, 0.0)

        // Activate a semester so filteredHistory doesn't return emptyList()
        semesterRepo.insertVersion(
            SemesterVersion(
                id = 1L,
                name = "Test Semester",
                isActive = true,
                startDate = LocalDate.now().minusMonths(3),
                endDate = LocalDate.now().plusMonths(3)
            )
        )
        semesterRepo.switchActiveVersion(1L)
        advanceUntilIdle()

        // Add subjects and schedules
        val subjId = subjectRepo.insertSubject(Subject(id = 1L, name = "SE"))
        // Mark 4 present, 0 absent (100%) and one Medical Leave record.
        attendanceRepo.insertAttendance(Attendance(id = 1L, subjectId = subjId, scheduleId = 0L, date = LocalDate.now(), status = AttendanceStatus.PRESENT))
        attendanceRepo.insertAttendance(Attendance(id = 2L, subjectId = subjId, scheduleId = 0L, date = LocalDate.now(), status = AttendanceStatus.PRESENT))
        attendanceRepo.insertAttendance(Attendance(id = 3L, subjectId = subjId, scheduleId = 0L, date = LocalDate.now(), status = AttendanceStatus.PRESENT))
        attendanceRepo.insertAttendance(Attendance(id = 4L, subjectId = subjId, scheduleId = 0L, date = LocalDate.now(), status = AttendanceStatus.PRESENT))
        attendanceRepo.insertAttendance(Attendance(id = 5L, subjectId = subjId, scheduleId = 0L, date = LocalDate.now(), status = AttendanceStatus.MEDICAL_LEAVE))
        advanceUntilIdle()

        // With 75% goal, 4 Present, 1 Medical Leave (80.0% normal), limit is (400 - 375)/75 = 0 safe misses, so status is WARNING
        assertEquals(80.0, viewModel.dashboardStats.value!!.overallPercentage, 0.1)
        assertEquals(100.0, viewModel.dashboardStats.value!!.withMedicalPercentage, 0.1)
        assertEquals("WARNING", viewModel.dashboardStats.value!!.safetyStatus)

        // Now change target goal to 90%
        settingsRepo.updateAttendanceTarget(com.attendance.tracker.core.model.AttendanceTarget(90.0, 90.0))
        advanceUntilIdle()

        // With 90% goal and 80.0% normal attendance, percentage < goal so safety status becomes CRITICAL
        assertEquals("CRITICAL", viewModel.dashboardStats.value!!.safetyStatus)
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

        // 3. Mark an unmarked class as MEDICAL_LEAVE
        viewModel.onAttendanceStatusClick(item, AttendanceStatus.MEDICAL_LEAVE)
        advanceUntilIdle()
        val markedItem = viewModel.todayClasses.value.first()
        assertNotNull(markedItem.attendance)
        assertEquals(AttendanceStatus.MEDICAL_LEAVE, markedItem.attendance!!.status)

        // 4. Transition Medical Leave through the other statuses
        viewModel.onAttendanceStatusClick(markedItem, AttendanceStatus.PRESENT)
        advanceUntilIdle()
        assertEquals(AttendanceStatus.PRESENT, viewModel.todayClasses.value.first().attendance!!.status)

        viewModel.onAttendanceStatusClick(viewModel.todayClasses.value.first(), AttendanceStatus.ABSENT)
        advanceUntilIdle()
        assertEquals(AttendanceStatus.ABSENT, viewModel.todayClasses.value.first().attendance!!.status)

        viewModel.onAttendanceStatusClick(viewModel.todayClasses.value.first(), AttendanceStatus.CANCELLED)
        advanceUntilIdle()
        assertEquals(AttendanceStatus.CANCELLED, viewModel.todayClasses.value.first().attendance!!.status)

        viewModel.onAttendanceStatusClick(viewModel.todayClasses.value.first(), AttendanceStatus.MEDICAL_LEAVE)
        advanceUntilIdle()
        val changedItem = viewModel.todayClasses.value.first()
        assertNotNull(changedItem.attendance)
        assertEquals(AttendanceStatus.MEDICAL_LEAVE, changedItem.attendance!!.status)

        // 5. Click Medical Leave again to clear selection (delete attendance)
        viewModel.onAttendanceStatusClick(changedItem, AttendanceStatus.MEDICAL_LEAVE)
        advanceUntilIdle()
        val clearedItem = viewModel.todayClasses.value.first()
        assertEquals(null, clearedItem.attendance)
    }

    @Test
    fun testSubjectStatsList_usesSubjectSpecificGoals() = runTest {
        semesterRepo.insertVersion(
            SemesterVersion(
                id = 1L,
                name = "Test Semester",
                isActive = true,
                startDate = LocalDate.now().minusMonths(3),
                endDate = LocalDate.now().plusMonths(3)
            )
        )
        semesterRepo.switchActiveVersion(1L)
        advanceUntilIdle()

        // Subject A has personal goal 50%, Subject B has personal goal 75%
        val subA = subjectRepo.insertSubject(Subject(id = 1L, name = "SubA", personalAttendanceGoal = 50, requiredAttendancePercentage = 50))
        val subB = subjectRepo.insertSubject(Subject(id = 2L, name = "SubB", personalAttendanceGoal = 75, requiredAttendancePercentage = 75))

        // SubB has P=0, A=1, ML=2 -> displayTotal=3, normal=0%
        attendanceRepo.insertAttendance(Attendance(id = 10L, subjectId = subB, scheduleId = 0L, date = LocalDate.now(), status = AttendanceStatus.ABSENT))
        attendanceRepo.insertAttendance(Attendance(id = 11L, subjectId = subB, scheduleId = 0L, date = LocalDate.now(), status = AttendanceStatus.MEDICAL_LEAVE))
        attendanceRepo.insertAttendance(Attendance(id = 12L, subjectId = subB, scheduleId = 0L, date = LocalDate.now(), status = AttendanceStatus.MEDICAL_LEAVE))
        advanceUntilIdle()

        val subjectStats = viewModel.subjectStatsList.value
        val statA = subjectStats.find { it.subjectId == subA }
        val statB = subjectStats.find { it.subjectId == subB }

        assertNotNull(statA)
        assertNotNull(statB)

        assertEquals(50, statA!!.personalGoalPercentage)
        assertEquals(75, statB!!.personalGoalPercentage)
        assertEquals(9, statB.classesNeeded) // (0 + 9)/(3 + 9) = 75% goal, NOT 1
    }
}
