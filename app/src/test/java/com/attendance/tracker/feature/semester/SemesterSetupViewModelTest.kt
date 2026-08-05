package com.attendance.tracker.feature.semester

import com.attendance.tracker.core.model.AttendanceTarget
import com.attendance.tracker.domain.model.SemesterVersion
import com.attendance.tracker.domain.repository.AttendanceRepository
import com.attendance.tracker.domain.repository.ScheduleRepository
import com.attendance.tracker.domain.repository.SemesterRepository
import com.attendance.tracker.domain.repository.SubjectRepository
import com.attendance.tracker.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SemesterSetupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var semesterRepo: SemesterRepository
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var scheduleRepo: ScheduleRepository
    private lateinit var attendanceRepo: AttendanceRepository
    private lateinit var subjectRepo: SubjectRepository

    private lateinit var viewModel: SemesterSetupViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        semesterRepo = mock(SemesterRepository::class.java)
        settingsRepo = mock(SettingsRepository::class.java)
        scheduleRepo = mock(ScheduleRepository::class.java)
        attendanceRepo = mock(AttendanceRepository::class.java)
        subjectRepo = mock(SubjectRepository::class.java)

        `when`(settingsRepo.getAttendanceTarget()).thenReturn(flowOf(AttendanceTarget(75.0, 80.0)))
        `when`(subjectRepo.observeSubjects()).thenReturn(flowOf(emptyList()))
        `when`(semesterRepo.observeActiveVersion()).thenReturn(flowOf(null))
        `when`(attendanceRepo.observeAttendanceHistory()).thenReturn(flowOf(emptyList()))

        viewModel = SemesterSetupViewModel(
            semesterRepository = semesterRepo,
            settingsRepository = settingsRepo,
            scheduleRepository = scheduleRepo,
            attendanceRepository = attendanceRepo,
            subjectRepository = subjectRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState_loadsTargetSettings() = runTest {
        advanceUntilIdle()
        assertEquals(SetupStep.DETAILS, viewModel.currentStep.value)
        assertEquals("Semester 1", viewModel.semesterName.value)
        assertEquals(80, viewModel.attendanceGoal.value)
    }

    @Test
    fun testSetStep_updatesStepCorrectly() {
        viewModel.setStep(SetupStep.TIMETABLE)
        assertEquals(SetupStep.TIMETABLE, viewModel.currentStep.value)
    }

    @Test
    fun testSetSemesterName_updatesNameCorrectly() {
        viewModel.setSemesterName("Fall 2026")
        assertEquals("Fall 2026", viewModel.semesterName.value)
    }

    @Test
    fun testSetStartDate_updatesDatesAndEndOffset() {
        val start = LocalDate.of(2026, 9, 1)
        viewModel.setEndDate(start.minusDays(1))
        viewModel.setStartDate(start)
        assertEquals(start, viewModel.startDate.value)
        assertEquals(start.plusMonths(4), viewModel.endDate.value)
    }
}
