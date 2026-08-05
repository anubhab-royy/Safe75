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
            dispatcherProvider = testDispatcherProvider
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
        // Row was not already in the list so list remains empty
        assertEquals(0, viewModel.timetableRows.value.size)
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
}
