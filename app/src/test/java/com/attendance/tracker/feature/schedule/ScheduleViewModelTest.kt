package com.attendance.tracker.feature.schedule

import com.attendance.tracker.core.common.UiState
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.domain.model.SemesterVersion
import com.attendance.tracker.domain.usecase.attendance.DetectMissingAttendanceUseCase
import com.attendance.tracker.domain.usecase.planner.LocalFakeAttendanceRepository
import com.attendance.tracker.domain.usecase.schedule.AddScheduleUseCase
import com.attendance.tracker.domain.usecase.schedule.DeleteScheduleUseCase
import com.attendance.tracker.domain.usecase.schedule.DetectConflictUseCase
import com.attendance.tracker.domain.usecase.schedule.FakeScheduleRepository
import com.attendance.tracker.domain.usecase.schedule.FakeSemesterRepository
import com.attendance.tracker.domain.usecase.schedule.GetWeekScheduleUseCase
import com.attendance.tracker.domain.usecase.schedule.ObserveScheduleUseCase
import com.attendance.tracker.domain.usecase.schedule.SaveMultiDayScheduleUseCase
import com.attendance.tracker.domain.usecase.schedule.SwitchTimetableVersionUseCase
import com.attendance.tracker.domain.usecase.schedule.UpdateScheduleUseCase
import com.attendance.tracker.domain.usecase.subject.FakeSubjectRepository
import com.attendance.tracker.domain.validation.ScheduleValidator
import com.attendance.tracker.domain.validation.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var scheduleRepo: FakeScheduleRepository
    private lateinit var semesterRepo: FakeSemesterRepository
    private lateinit var subjectRepo: FakeSubjectRepository
    private lateinit var attendanceRepo: LocalFakeAttendanceRepository
    private lateinit var viewModel: ScheduleViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        scheduleRepo = FakeScheduleRepository()
        semesterRepo = FakeSemesterRepository()
        subjectRepo = FakeSubjectRepository()
        attendanceRepo = LocalFakeAttendanceRepository()

        val observeUseCase = ObserveScheduleUseCase(scheduleRepo)
        val getWeekUseCase = GetWeekScheduleUseCase(scheduleRepo)
        val addUseCase = AddScheduleUseCase(scheduleRepo)
        val updateUseCase = UpdateScheduleUseCase(scheduleRepo)
        val deleteUseCase = DeleteScheduleUseCase(scheduleRepo)
        val detectConflict = DetectConflictUseCase(scheduleRepo)
        val switchVersion = SwitchTimetableVersionUseCase(semesterRepo)
        val validator = ScheduleValidator()

        viewModel = ScheduleViewModel(
            observeScheduleUseCase = observeUseCase,
            getWeekScheduleUseCase = getWeekUseCase,
            addScheduleUseCase = addUseCase,
            updateScheduleUseCase = updateUseCase,
            deleteScheduleUseCase = deleteUseCase,
            saveMultiDayScheduleUseCase = SaveMultiDayScheduleUseCase(scheduleRepo),
            detectConflictUseCase = detectConflict,
            switchTimetableVersionUseCase = switchVersion,
            semesterRepository = semesterRepo,
            subjectRepository = subjectRepo,
            scheduleRepository = scheduleRepo,
            attendanceRepository = attendanceRepo,
            detectMissingAttendanceUseCase = DetectMissingAttendanceUseCase(),
            validator = validator
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState_createsDefaultTimetableVersion() = runTest {
        advanceUntilIdle()
        // Default version should have been created as none existed
        val active = viewModel.activeVersion.value
        assertTrue(active != null)
        assertEquals("Semester 1", active?.name)
        assertTrue(semesterRepo.versions.isNotEmpty())
    }

    @Test
    fun testSaveOverlappingSchedule_failsAndSetsConflictState() = runTest {
        advanceUntilIdle()
        val activeVer = viewModel.activeVersion.value ?: throw IllegalStateException()

        // Create an existing schedule
        val existing = com.attendance.tracker.domain.model.Schedule(
            id = 1L,
            subjectId = 10L,
            dayOfWeek = WeekDay.Monday,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 30),
            versionId = activeVer.id
        )
        scheduleRepo.list.add(existing)

        // Try to save an overlapping schedule (09:30 to 10:30)
        val success = viewModel.saveSchedule(
            id = 0L,
            subjectId = 11L,
            day = WeekDay.Monday,
            startTime = LocalTime.of(9, 30),
            endTime = LocalTime.of(10, 30),
            room = "302",
            teacher = "Prof"
        )
        advanceUntilIdle()

        assertTrue(!success)
        assertTrue(viewModel.conflicts.value.isNotEmpty())
        assertTrue(viewModel.validationState.value is ValidationResult.Invalid)
        assertEquals("Timing overlap conflict detected", (viewModel.validationState.value as ValidationResult.Invalid).reason)
    }

    @Test
    fun testSearchSchedules_filtersCorrectly() = runTest {
        advanceUntilIdle()
        val activeVer = viewModel.activeVersion.value ?: throw IllegalStateException()

        val s1 = com.attendance.tracker.domain.model.Schedule(
            id = 1L,
            subjectId = 1L,
            dayOfWeek = WeekDay.Monday,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0),
            room = "Lab 1",
            versionId = activeVer.id
        )
        val s2 = com.attendance.tracker.domain.model.Schedule(
            id = 2L,
            subjectId = 2L,
            dayOfWeek = WeekDay.Monday,
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            room = "Room 303",
            versionId = activeVer.id
        )
        scheduleRepo.list.addAll(listOf(s1, s2))

        val subj1 = com.attendance.tracker.domain.model.Subject(id = 1L, name = "Physics")
        val subj2 = com.attendance.tracker.domain.model.Subject(id = 2L, name = "Chemistry")
        subjectRepo.subjects.addAll(listOf(subj1, subj2))

        // Trigger search query
        viewModel.onSearchQueryChange("Physics")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals(1, data.size)
        assertEquals("Physics", data.first().subjectName)
    }

    @Test
    fun testSwitchVersion_loadsNewSchedules() = runTest {
        advanceUntilIdle()
        // Create another version
        viewModel.createTimetableVersion("Semester 2")
        advanceUntilIdle()

        val active = viewModel.activeVersion.value
        assertEquals("Semester 2", active?.name)
    }

    @Test
    fun testSaveScheduleMultiDay_persistsOneRecordPerDay() = runTest {
        advanceUntilIdle()
        val activeVer = viewModel.activeVersion.value ?: throw IllegalStateException()

        val success = viewModel.saveScheduleMultiDay(
            id = 0L,
            subjectId = 10L,
            days = setOf(WeekDay.Monday, WeekDay.Wednesday, WeekDay.Friday),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 30),
            room = "301",
            teacher = "Prof A"
        )
        advanceUntilIdle()

        assertTrue(success)
        val saved = scheduleRepo.list.filter { it.subjectId == 10L }
        assertEquals(3, saved.size)
        assertEquals(
            setOf(WeekDay.Monday, WeekDay.Wednesday, WeekDay.Friday),
            saved.map { it.dayOfWeek }.toSet()
        )
        assertTrue(saved.all { it.versionId == activeVer.id })
    }

    @Test
    fun testSaveScheduleMultiDay_emptySelectionFailsValidation() = runTest {
        advanceUntilIdle()

        val success = viewModel.saveScheduleMultiDay(
            id = 0L,
            subjectId = 10L,
            days = emptySet(),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 30),
            room = "301",
            teacher = "Prof A"
        )
        advanceUntilIdle()

        assertTrue(!success)
        assertTrue(viewModel.validationState.value is ValidationResult.Invalid)
        assertEquals(
            "Select at least one day of the week",
            (viewModel.validationState.value as ValidationResult.Invalid).reason
        )
    }

    @Test
    fun testSaveScheduleMultiDay_editRemovesUnselectedSibling() = runTest {
        advanceUntilIdle()
        val activeVer = viewModel.activeVersion.value ?: throw IllegalStateException()

        val monday = com.attendance.tracker.domain.model.Schedule(
            id = 1L,
            subjectId = 20L,
            dayOfWeek = WeekDay.Monday,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 30),
            versionId = activeVer.id
        )
        val wednesday = com.attendance.tracker.domain.model.Schedule(
            id = 2L,
            subjectId = 20L,
            dayOfWeek = WeekDay.Wednesday,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 30),
            versionId = activeVer.id
        )
        scheduleRepo.list.addAll(listOf(monday, wednesday))

        // Edit the Monday record, keeping only Wednesday
        val success = viewModel.saveScheduleMultiDay(
            id = 1L,
            subjectId = 20L,
            days = setOf(WeekDay.Wednesday),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 30),
            room = null,
            teacher = null
        )
        advanceUntilIdle()

        assertTrue(success)
        val remaining = scheduleRepo.list.filter { it.subjectId == 20L }
        assertEquals(1, remaining.size)
        assertEquals(WeekDay.Wednesday, remaining.single().dayOfWeek)
    }

    @Test
    fun testSaveScheduleMultiDay_triggersBackfillPromptForMissingHistory() = runTest {
        advanceUntilIdle()
        val activeVer = viewModel.activeVersion.value ?: throw IllegalStateException()

        // Move the semester start date into the past so history can be detected
        semesterRepo.updateVersion(activeVer.copy(startDate = LocalDate.now().minusDays(60)))
        advanceUntilIdle()

        subjectRepo.subjects.add(com.attendance.tracker.domain.model.Subject(id = 30L, name = "Math"))
        val success = viewModel.saveScheduleMultiDay(
            id = 0L,
            subjectId = 30L,
            days = setOf(WeekDay.Friday),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0),
            room = "101",
            teacher = null
        )
        advanceUntilIdle()

        assertTrue(success)
        val prompt = viewModel.pendingBackfillPrompt.value
        assertTrue(prompt != null)
        assertEquals(30L, prompt?.subjectId)
        assertEquals("Math", prompt?.subjectName)
        assertTrue(prompt?.missingCount ?: 0 > 0)
    }
}
