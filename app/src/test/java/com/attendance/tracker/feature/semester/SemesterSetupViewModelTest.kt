package com.attendance.tracker.feature.semester

import com.attendance.tracker.core.model.AttendanceTarget
import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.model.SemesterVersion
import com.attendance.tracker.domain.repository.AttendanceRepository
import com.attendance.tracker.domain.repository.ScheduleRepository
import com.attendance.tracker.domain.repository.SemesterRepository
import com.attendance.tracker.domain.repository.SubjectRepository
import com.attendance.tracker.domain.repository.SettingsRepository
import com.attendance.tracker.domain.usecase.attendance.DeleteAttendanceUseCase
import com.attendance.tracker.domain.usecase.attendance.MarkAttendanceUseCase
import com.attendance.tracker.domain.usecase.attendance.UpdateAttendanceUseCase
import com.attendance.tracker.domain.validation.AttendanceValidator
import com.attendance.tracker.domain.usecase.planner.LocalFakeAttendanceRepository
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
    private lateinit var attendanceRepo: LocalFakeAttendanceRepository
    private lateinit var subjectRepo: SubjectRepository

    private lateinit var viewModel: SemesterSetupViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        semesterRepo = mock(SemesterRepository::class.java)
        settingsRepo = mock(SettingsRepository::class.java)
        scheduleRepo = mock(ScheduleRepository::class.java)
        attendanceRepo = LocalFakeAttendanceRepository()
        subjectRepo = mock(SubjectRepository::class.java)

        `when`(settingsRepo.getAttendanceTarget()).thenReturn(flowOf(AttendanceTarget(75.0, 80.0)))
        `when`(subjectRepo.observeSubjects()).thenReturn(flowOf(emptyList()))
        `when`(semesterRepo.observeActiveVersion()).thenReturn(flowOf(null))
        val validator = AttendanceValidator()
        viewModel = SemesterSetupViewModel(
            semesterRepository = semesterRepo,
            settingsRepository = settingsRepo,
            scheduleRepository = scheduleRepo,
            attendanceRepository = attendanceRepo,
            subjectRepository = subjectRepo,
            markAttendanceUseCase = MarkAttendanceUseCase(attendanceRepo, validator),
            updateAttendanceUseCase = UpdateAttendanceUseCase(attendanceRepo, validator),
            deleteAttendanceUseCase = DeleteAttendanceUseCase(attendanceRepo)
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

    @Test
    fun testMarkPastAttendance_createsMedicalLeaveRecord() = runTest {
        val item = PastClassItem(
            date = LocalDate.now().minusDays(1),
            scheduleId = 10L,
            subjectId = 1L,
            subjectName = "Mathematics",
            startTime = java.time.LocalTime.of(9, 0),
            endTime = java.time.LocalTime.of(10, 0),
            room = null,
            status = null
        )

        viewModel.markPastAttendance(item, AttendanceStatus.MEDICAL_LEAVE)
        advanceUntilIdle()

        val stored = attendanceRepo.getAttendanceForDateSync(item.date).single()
        assertEquals(AttendanceStatus.MEDICAL_LEAVE, stored.status)
        assertEquals(item.subjectId, stored.subjectId)
        assertEquals(item.scheduleId, stored.scheduleId)
        assertEquals(item.date, stored.date)
    }

    @Test
    fun testMarkPastAttendance_updatesExistingMedicalLeaveRecord() = runTest {
        val existing = Attendance(
            id = 7L,
            subjectId = 1L,
            scheduleId = 10L,
            date = LocalDate.now().minusDays(1),
            status = AttendanceStatus.MEDICAL_LEAVE,
            createdAt = 123L
        )
        attendanceRepo.insertAttendance(existing)
        advanceUntilIdle()

        val item = PastClassItem(
            date = existing.date,
            scheduleId = existing.scheduleId,
            subjectId = existing.subjectId,
            subjectName = "Mathematics",
            startTime = java.time.LocalTime.of(9, 0),
            endTime = java.time.LocalTime.of(10, 0),
            room = null,
            status = existing.status
        )

        viewModel.markPastAttendance(item, AttendanceStatus.PRESENT)
        advanceUntilIdle()

        val stored = attendanceRepo.getAttendanceById(existing.id)
        assertEquals(AttendanceStatus.PRESENT, stored?.status)
        assertEquals(existing.id, stored?.id)
        assertEquals(existing.createdAt, stored?.createdAt)
    }
}
