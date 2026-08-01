package com.attendance.tracker.feature.schedule

import com.attendance.tracker.core.common.UiState
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.domain.model.SemesterVersion
import com.attendance.tracker.domain.usecase.schedule.AddScheduleUseCase
import com.attendance.tracker.domain.usecase.schedule.DeleteScheduleUseCase
import com.attendance.tracker.domain.usecase.schedule.DetectConflictUseCase
import com.attendance.tracker.domain.usecase.schedule.FakeScheduleRepository
import com.attendance.tracker.domain.usecase.schedule.FakeSemesterRepository
import com.attendance.tracker.domain.usecase.schedule.GetWeekScheduleUseCase
import com.attendance.tracker.domain.usecase.schedule.ObserveScheduleUseCase
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
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var scheduleRepo: FakeScheduleRepository
    private lateinit var semesterRepo: FakeSemesterRepository
    private lateinit var subjectRepo: FakeSubjectRepository
    private lateinit var viewModel: ScheduleViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        scheduleRepo = FakeScheduleRepository()
        semesterRepo = FakeSemesterRepository()
        subjectRepo = FakeSubjectRepository()

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
            detectConflictUseCase = detectConflict,
            switchTimetableVersionUseCase = switchVersion,
            semesterRepository = semesterRepo,
            subjectRepository = subjectRepo,
            scheduleRepository = scheduleRepo,
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
}
