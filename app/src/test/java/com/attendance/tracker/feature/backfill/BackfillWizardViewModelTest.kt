package com.attendance.tracker.feature.backfill

import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.domain.model.SemesterVersion
import com.attendance.tracker.domain.model.Subject
import com.attendance.tracker.domain.usecase.attendance.DetectMissingAttendanceUseCase
import com.attendance.tracker.domain.usecase.planner.LocalFakeAttendanceRepository
import com.attendance.tracker.domain.usecase.schedule.FakeScheduleRepository
import com.attendance.tracker.domain.usecase.schedule.FakeSemesterRepository
import com.attendance.tracker.domain.usecase.subject.FakeSubjectRepository
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
class BackfillWizardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var scheduleRepo: FakeScheduleRepository
    private lateinit var semesterRepo: FakeSemesterRepository
    private lateinit var attendanceRepo: LocalFakeAttendanceRepository
    private lateinit var subjectRepo: FakeSubjectRepository
    private lateinit var viewModel: BackfillWizardViewModel

    private val today: LocalDate = LocalDate.now()
    private val todayWeekDay: WeekDay = WeekDay.fromJavaDayOfWeek(today.dayOfWeek)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        scheduleRepo = FakeScheduleRepository()
        semesterRepo = FakeSemesterRepository()
        attendanceRepo = LocalFakeAttendanceRepository()
        subjectRepo = FakeSubjectRepository()

        // Active version starting three weeks before today.
        semesterRepo.versions.add(
            SemesterVersion(
                id = 1L,
                name = "Semester 1",
                isActive = true,
                startDate = today.minusDays(21)
            )
        )
        subjectRepo.subjects.add(Subject(id = 1L, name = "Physics", color = 0xFF0000FF.toInt()))

        viewModel = BackfillWizardViewModel(
            scheduleRepository = scheduleRepo,
            semesterRepository = semesterRepo,
            attendanceRepository = attendanceRepo,
            subjectRepository = subjectRepo,
            detectMissingAttendanceUseCase = DetectMissingAttendanceUseCase()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun addWeeklySchedule(subjectId: Long, id: Long = subjectId) {
        scheduleRepo.list.add(
            Schedule(
                id = id,
                subjectId = subjectId,
                dayOfWeek = todayWeekDay,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(10, 30),
                versionId = 1L,
                room = "101"
            )
        )
    }

    @Test
    fun missingItems_isEmpty_whenNoSchedulesExist() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.missingItems.value.isEmpty())
    }

    @Test
    fun missingItems_listsEveryOccurrenceSinceSemesterStart() = runTest {
        addWeeklySchedule(subjectId = 1L)
        advanceUntilIdle()

        val items = viewModel.missingItems.value
        // Weeks at -21, -14, -7 and today
        assertEquals(4, items.size)
        assertEquals(
            setOf(today, today.minusDays(7), today.minusDays(14), today.minusDays(21)),
            items.map { it.date }.toSet()
        )
        assertTrue(items.all { it.subjectName == "Physics" })
        assertTrue(items.all { it.room == "101" })
    }

    @Test
    fun missingItems_skipsDatesWithExistingAttendance() = runTest {
        addWeeklySchedule(subjectId = 1L)
        attendanceRepo.insertAttendance(
            com.attendance.tracker.domain.model.Attendance(
                subjectId = 1L,
                scheduleId = 1L,
                date = today.minusDays(7),
                status = AttendanceStatus.PRESENT
            )
        )
        advanceUntilIdle()

        val items = viewModel.missingItems.value
        assertEquals(3, items.size)
        assertTrue(items.none { it.date == today.minusDays(7) })
    }

    @Test
    fun subjectFilter_restrictsItemsToOneSubject() = runTest {
        subjectRepo.subjects.add(Subject(id = 2L, name = "Chemistry"))
        addWeeklySchedule(subjectId = 1L, id = 1L)
        addWeeklySchedule(subjectId = 2L, id = 2L)
        viewModel.setSubjectFilter(1L)
        advanceUntilIdle()

        val items = viewModel.missingItems.value
        assertTrue(items.isNotEmpty())
        assertTrue(items.all { it.subjectId == 1L })
    }

    @Test
    fun markAttendance_persistsAndRemovesItemFromList() = runTest {
        addWeeklySchedule(subjectId = 1L)
        advanceUntilIdle()

        val target = viewModel.missingItems.value.first()
        viewModel.markAttendance(target, AttendanceStatus.PRESENT)
        advanceUntilIdle()

        assertEquals(3, viewModel.missingItems.value.size)
        assertEquals(1, attendanceRepo.getAttendanceForSubject(1L).size)
        assertEquals(AttendanceStatus.PRESENT, attendanceRepo.getAttendanceForSubject(1L).single().status)
    }

    @Test
    fun markAttendance_tappingSameStatusAgainRemovesRecord() = runTest {
        addWeeklySchedule(subjectId = 1L)
        advanceUntilIdle()

        val target = viewModel.missingItems.value.first()
        viewModel.markAttendance(target, AttendanceStatus.ABSENT)
        advanceUntilIdle()
        assertEquals(3, viewModel.missingItems.value.size)

        viewModel.markAttendance(target, AttendanceStatus.ABSENT)
        advanceUntilIdle()
        assertEquals(4, viewModel.missingItems.value.size)
        assertTrue(attendanceRepo.getAttendanceForSubject(1L).isEmpty())
    }

    @Test
    fun markAll_appliesStatusToEveryMissingItem() = runTest {
        addWeeklySchedule(subjectId = 1L)
        advanceUntilIdle()

        viewModel.markAll(AttendanceStatus.CANCELLED)
        advanceUntilIdle()

        assertTrue(viewModel.missingItems.value.isEmpty())
        val recorded = attendanceRepo.getAttendanceForSubject(1L)
        assertEquals(4, recorded.size)
        assertTrue(recorded.all { it.status == AttendanceStatus.CANCELLED })
    }
}
