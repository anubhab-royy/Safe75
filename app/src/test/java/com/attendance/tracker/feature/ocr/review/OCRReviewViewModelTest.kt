package com.attendance.tracker.feature.ocr.review

import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.domain.model.SemesterVersion
import com.attendance.tracker.domain.model.Subject
import com.attendance.tracker.domain.usecase.planner.LocalFakeAttendanceRepository
import com.attendance.tracker.domain.usecase.planner.LocalFakeScheduleRepository
import com.attendance.tracker.domain.usecase.planner.LocalFakeSubjectRepository
import com.attendance.tracker.feature.ocr.model.OcrField
import com.attendance.tracker.feature.ocr.model.OcrTimetableRow
import com.attendance.tracker.feature.ocr.model.OcrAttendanceRow
import com.attendance.tracker.feature.ocr.parser.OcrParser
import com.attendance.tracker.feature.ocr.repository.OcrRepository
import com.attendance.tracker.feature.ocr.scanner.OcrScanner
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

// ---------------------------------------------------------------------------
// Local fake SemesterRepository (self-contained, no external imports needed)
// ---------------------------------------------------------------------------
private class FakeOcrSemesterRepository : com.attendance.tracker.domain.repository.SemesterRepository {
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
        _list.value = _list.value.map { it.copy(isActive = it.id == versionId) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class OCRReviewViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var subjectRepo: LocalFakeSubjectRepository
    private lateinit var scheduleRepo: LocalFakeScheduleRepository
    private lateinit var semesterRepo: FakeOcrSemesterRepository
    private lateinit var attendanceRepo: LocalFakeAttendanceRepository
    private lateinit var viewModel: OcrReviewViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        subjectRepo = LocalFakeSubjectRepository()
        scheduleRepo = LocalFakeScheduleRepository()
        semesterRepo = FakeOcrSemesterRepository()
        attendanceRepo = LocalFakeAttendanceRepository()

        val testDispatcherProvider = object : com.attendance.tracker.core.common.DispatcherProvider {
            override val main = testDispatcher
            override val io = testDispatcher
            override val default = testDispatcher
        }

        val ocrRepo = OcrRepository(
            scanner = OcrScanner(),
            parser = OcrParser(),
            imageProcessor = com.attendance.tracker.feature.ocr.processing.ImageProcessor(),
            tableDetector = com.attendance.tracker.feature.ocr.detection.TableDetector(),
            gridDetector = com.attendance.tracker.feature.ocr.structure.OpenCVGridDetector(),
            cellExtractor = com.attendance.tracker.feature.ocr.extraction.CellExtractor(),
            ocrRecognizer = com.attendance.tracker.feature.ocr.recognition.OcrRecognizer(OcrScanner()),
            semanticParser = com.attendance.tracker.feature.ocr.parser.SemanticParser(
                com.attendance.tracker.feature.ocr.validation.ValidationEngine(),
                com.attendance.tracker.feature.ocr.parser.HeaderInterpreter(),
                kotlinx.serialization.json.Json { prettyPrint = true }
            ),
            dispatcherProvider = testDispatcherProvider,
            openCVInitializer = com.attendance.tracker.core.opencv.OpenCVInitializer()
        )

        viewModel = OcrReviewViewModel(
            ocrRepository = ocrRepo,
            subjectRepository = subjectRepo,
            scheduleRepository = scheduleRepo,
            semesterRepository = semesterRepo,
            attendanceRepository = attendanceRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState_isEmpty() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.timetableRows.value.isEmpty())
        assertTrue(viewModel.attendanceRows.value.isEmpty())
    }

    @Test
    fun testUpdateTimetableRow_whenRowPresent_modifiesState() = runTest {
        advanceUntilIdle()
        // Since rows start empty, update does nothing — state stays empty
        val updatedRow = OcrTimetableRow(
            id = "row-1",
            subjectName = OcrField("Updated Maths", 0.98f),
            dayOfWeek = OcrField("Monday", 0.98f),
            startTime = OcrField("09:00", 0.98f),
            endTime = OcrField("10:00", 0.98f)
        )
        viewModel.updateTimetableRow(updatedRow)
        assertEquals(1, viewModel.timetableRows.value.size)
        assertEquals("Updated Maths", viewModel.timetableRows.value.first().subjectName.value)
    }

    @Test
    fun testDeleteTimetableRow_whenRowNotPresent_stateUnchanged() = runTest {
        advanceUntilIdle()
        viewModel.deleteTimetableRow("nonexistent-id")
        assertTrue(viewModel.timetableRows.value.isEmpty())
    }

    @Test
    fun testDeleteAttendanceRow_whenRowNotPresent_stateUnchanged() = runTest {
        advanceUntilIdle()
        viewModel.deleteAttendanceRow("nonexistent-id")
        assertTrue(viewModel.attendanceRows.value.isEmpty())
    }

    @Test
    fun testIsLoadingInitiallyFalse() = runTest {
        advanceUntilIdle()
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun testErrorInitiallyNull() = runTest {
        advanceUntilIdle()
        assertEquals(null, viewModel.error.value)
    }

    @Test
    fun testSaveTimetable_withNoActiveSemester_returnsFalse() = runTest {
        advanceUntilIdle()
        // No active semester inserted, save should return false
        val result = viewModel.saveTimetable()
        assertEquals(false, result)
    }

    @Test
    fun testSaveAttendance_withNoRows_returnsTrue() = runTest {
        advanceUntilIdle()
        // Empty rows, save attendance has nothing to do — returns true
        val result = viewModel.saveAttendance(emptyMap())
        assertEquals(true, result)
    }

    @Test
    fun testSaveAttendance_validSingleScheduleResolution_savesSuccessfullyWithRealScheduleId() = runTest {
        advanceUntilIdle()
        semesterRepo.insertVersion(SemesterVersion(id = 1L, name = "Fall 2026", startDate = LocalDate.now().minusMonths(3), endDate = LocalDate.now().plusMonths(3), isActive = true))
        val subId = subjectRepo.insertSubject(Subject(id = 1L, name = "Mathematics", requiredAttendancePercentage = 75, personalAttendanceGoal = 85))

        // Add schedule for every weekday so date walk succeeds
        for (day in WeekDay.entries) {
            scheduleRepo.insertSchedule(Schedule(id = day.ordinal + 10L, subjectId = subId, dayOfWeek = day, startTime = java.time.LocalTime.of(9, 0), endTime = java.time.LocalTime.of(10, 0), versionId = 1L))
        }

        viewModel.updateAttendanceRow(
            OcrAttendanceRow(
                id = "row-1",
                subjectName = OcrField("Mathematics", 0.95f),
                presentCount = OcrField(2, 0.95f),
                totalClasses = OcrField(2, 0.95f),
                percentage = OcrField(100.0, 0.95f),
                matchedSubjectId = subId
            )
        )

        val success = viewModel.saveAttendance(emptyMap())
        advanceUntilIdle()

        assertTrue(success)
        assertEquals(null, viewModel.error.value)
        val savedList = attendanceRepo.getAttendanceForSubject(subId)
        assertEquals(2, savedList.size)
        assertTrue(savedList.all { it.scheduleId != 0L })
    }

    @Test
    fun testSaveAttendance_missingScheduleMapping_returnsFalseWithUserError() = runTest {
        advanceUntilIdle()
        semesterRepo.insertVersion(SemesterVersion(id = 1L, name = "Fall 2026", startDate = LocalDate.now().minusMonths(3), endDate = LocalDate.now().plusMonths(3), isActive = true))
        val subId = subjectRepo.insertSubject(Subject(id = 1L, name = "Physics", requiredAttendancePercentage = 75, personalAttendanceGoal = 85))
        // No schedule added for Physics

        viewModel.updateAttendanceRow(
            OcrAttendanceRow(
                id = "row-1",
                subjectName = OcrField("Physics", 0.95f),
                presentCount = OcrField(1, 0.95f),
                totalClasses = OcrField(1, 0.95f),
                percentage = OcrField(100.0, 0.95f),
                matchedSubjectId = subId
            )
        )

        val success = viewModel.saveAttendance(emptyMap())
        advanceUntilIdle()

        assertEquals(false, success)
        assertTrue(viewModel.error.value?.contains("No timetable schedule found") == true)
    }

    @Test
    fun testSaveAttendance_multipleSchedulesAmbiguous_returnsFalseWithUserError() = runTest {
        advanceUntilIdle()
        semesterRepo.insertVersion(SemesterVersion(id = 1L, name = "Fall 2026", startDate = LocalDate.now().minusMonths(3), endDate = LocalDate.now().plusMonths(3), isActive = true))
        val subId = subjectRepo.insertSubject(Subject(id = 1L, name = "Chemistry", requiredAttendancePercentage = 75, personalAttendanceGoal = 85))

        // Insert two schedules for Chemistry on Monday
        scheduleRepo.insertSchedule(Schedule(id = 101L, subjectId = subId, dayOfWeek = WeekDay.Monday, startTime = java.time.LocalTime.of(9, 0), endTime = java.time.LocalTime.of(10, 0), versionId = 1L))
        scheduleRepo.insertSchedule(Schedule(id = 102L, subjectId = subId, dayOfWeek = WeekDay.Monday, startTime = java.time.LocalTime.of(14, 0), endTime = java.time.LocalTime.of(15, 0), versionId = 1L))

        viewModel.updateAttendanceRow(
            OcrAttendanceRow(
                id = "row-1",
                subjectName = OcrField("Chemistry", 0.95f),
                presentCount = OcrField(1, 0.95f),
                totalClasses = OcrField(1, 0.95f),
                percentage = OcrField(100.0, 0.95f),
                matchedSubjectId = subId
            )
        )

        val success = viewModel.saveAttendance(emptyMap())
        advanceUntilIdle()

        assertEquals(false, success)
        assertTrue(viewModel.error.value?.contains("Multiple timetable schedules found") == true)
    }

    @Test
    fun testSaveAttendance_existingOcrRecord_updatesStatus() = runTest {
        advanceUntilIdle()
        semesterRepo.insertVersion(SemesterVersion(id = 1L, name = "Fall 2026", startDate = LocalDate.now().minusMonths(3), endDate = LocalDate.now().plusMonths(3), isActive = true))
        val subId = subjectRepo.insertSubject(Subject(id = 1L, name = "Biology", requiredAttendancePercentage = 75, personalAttendanceGoal = 85))

        for (day in WeekDay.entries) {
            scheduleRepo.insertSchedule(Schedule(id = day.ordinal + 10L, subjectId = subId, dayOfWeek = day, startTime = java.time.LocalTime.of(9, 0), endTime = java.time.LocalTime.of(10, 0), versionId = 1L))
        }

        // Save initial OCR attendance (1 total class, 0 present -> ABSENT)
        viewModel.updateAttendanceRow(
            OcrAttendanceRow(
                id = "row-1",
                subjectName = OcrField("Biology", 0.95f),
                presentCount = OcrField(0, 0.95f),
                totalClasses = OcrField(1, 0.95f),
                percentage = OcrField(0.0, 0.95f),
                matchedSubjectId = subId
            )
        )
        viewModel.saveAttendance(emptyMap())
        advanceUntilIdle()

        val initialList = attendanceRepo.getAttendanceForSubject(subId)
        assertEquals(1, initialList.size)
        assertEquals(AttendanceStatus.ABSENT, initialList.first().status)

        // Save updated OCR attendance (1 total class, 1 present -> PRESENT)
        viewModel.updateAttendanceRow(
            OcrAttendanceRow(
                id = "row-1",
                subjectName = OcrField("Biology", 0.95f),
                presentCount = OcrField(1, 0.95f),
                totalClasses = OcrField(1, 0.95f),
                percentage = OcrField(100.0, 0.95f),
                matchedSubjectId = subId
            )
        )
        val success = viewModel.saveAttendance(emptyMap())
        advanceUntilIdle()

        assertTrue(success)
        val updatedList = attendanceRepo.getAttendanceForSubject(subId)
        assertEquals(1, updatedList.size)
        assertEquals(AttendanceStatus.PRESENT, updatedList.first().status)
    }

    @Test
    fun testSaveAttendance_existingManualRecord_preservesManualEntry() = runTest {
        advanceUntilIdle()
        semesterRepo.insertVersion(SemesterVersion(id = 1L, name = "Fall 2026", startDate = LocalDate.now().minusMonths(3), endDate = LocalDate.now().plusMonths(3), isActive = true))
        val subId = subjectRepo.insertSubject(Subject(id = 1L, name = "History", requiredAttendancePercentage = 75, personalAttendanceGoal = 85))

        val schedId = scheduleRepo.insertSchedule(Schedule(id = 50L, subjectId = subId, dayOfWeek = WeekDay.Monday, startTime = java.time.LocalTime.of(9, 0), endTime = java.time.LocalTime.of(10, 0), versionId = 1L))
        for (day in WeekDay.entries) {
            if (day != WeekDay.Monday) {
                scheduleRepo.insertSchedule(Schedule(id = day.ordinal + 10L, subjectId = subId, dayOfWeek = day, startTime = java.time.LocalTime.of(9, 0), endTime = java.time.LocalTime.of(10, 0), versionId = 1L))
            }
        }

        // Insert a manual attendance record for today (or recent Monday)
        val mondayDate = LocalDate.now()
        attendanceRepo.insertAttendance(
            Attendance(id = 99L, subjectId = subId, scheduleId = schedId, date = mondayDate, status = AttendanceStatus.PRESENT, remarks = "Manual entry")
        )

        // Perform OCR import
        viewModel.updateAttendanceRow(
            OcrAttendanceRow(
                id = "row-1",
                subjectName = OcrField("History", 0.95f),
                presentCount = OcrField(0, 0.95f),
                totalClasses = OcrField(1, 0.95f),
                percentage = OcrField(0.0, 0.95f),
                matchedSubjectId = subId
            )
        )
        viewModel.saveAttendance(emptyMap())
        advanceUntilIdle()

        // Manual record must be preserved
        val record = attendanceRepo.getAttendanceById(99L)
        assertTrue(record != null)
        assertEquals("Manual entry", record?.remarks)
        assertEquals(AttendanceStatus.PRESENT, record?.status)
    }

    @Test
    fun testSaveAttendance_repeatedImport_doesNotCrash() = runTest {
        advanceUntilIdle()
        semesterRepo.insertVersion(SemesterVersion(id = 1L, name = "Fall 2026", startDate = LocalDate.now().minusMonths(3), endDate = LocalDate.now().plusMonths(3), isActive = true))
        val subId = subjectRepo.insertSubject(Subject(id = 1L, name = "CS101", requiredAttendancePercentage = 75, personalAttendanceGoal = 85))

        for (day in WeekDay.entries) {
            scheduleRepo.insertSchedule(Schedule(id = day.ordinal + 10L, subjectId = subId, dayOfWeek = day, startTime = java.time.LocalTime.of(9, 0), endTime = java.time.LocalTime.of(10, 0), versionId = 1L))
        }

        val row = OcrAttendanceRow(
            id = "row-1",
            subjectName = OcrField("CS101", 0.95f),
            presentCount = OcrField(2, 0.95f),
            totalClasses = OcrField(2, 0.95f),
            percentage = OcrField(100.0, 0.95f),
            matchedSubjectId = subId
        )

        viewModel.updateAttendanceRow(row)
        val success1 = viewModel.saveAttendance(emptyMap())
        advanceUntilIdle()
        assertTrue(success1)

        viewModel.updateAttendanceRow(row)
        val success2 = viewModel.saveAttendance(emptyMap())
        advanceUntilIdle()
        assertTrue(success2)
    }
}
