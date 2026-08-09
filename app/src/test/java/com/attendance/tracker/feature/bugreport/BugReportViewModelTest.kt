package com.attendance.tracker.feature.bugreport

import com.attendance.tracker.core.diagnostics.CrashReport
import com.attendance.tracker.core.diagnostics.DeviceInfo
import com.attendance.tracker.core.diagnostics.DiagnosticsBundle
import com.attendance.tracker.core.diagnostics.DiagnosticsExporter
import com.attendance.tracker.core.common.DispatcherProvider
import com.attendance.tracker.feature.bugreport.data.BugReportWorkScheduler
import com.attendance.tracker.feature.bugreport.data.ScreenshotContentReader
import com.attendance.tracker.feature.bugreport.domain.model.QueueEnqueueResult
import com.attendance.tracker.feature.bugreport.domain.repository.BugReportQueueRepository
import com.attendance.tracker.feature.bugreport.model.ScreenshotAttachment
import com.attendance.tracker.feature.device.domain.model.BugReportSubmission
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BugReportViewModelTest {
    private val repository = mockk<BugReportQueueRepository>()
    private val scheduler = mockk<BugReportWorkScheduler>(relaxed = true)
    private val exporter = mockk<DiagnosticsExporter>()
    private val reader = mockk<ScreenshotContentReader>()
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var viewModel: BugReportViewModel

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        viewModel = BugReportViewModel(
            repository,
            scheduler,
            exporter,
            reader,
            object : DispatcherProvider {
                override val main = testDispatcher
                override val io = testDispatcher
                override val default = testDispatcher
            }
        )
        every { exporter.buildBundle(any()) } returns diagnosticsBundle()
        every { exporter.toJson(any()) } returns "{\"diagnostics\":true}"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun submit_withoutScreenshot_generatesDiagnosticsAndSucceeds() = runTest {
        val request = slot<BugReportSubmission>()
        coEvery { repository.enqueue(capture(request), null) } returns Result.success(
            QueueEnqueueResult("report-1", alreadyQueued = false)
        )

        viewModel.onDescriptionChanged("The dashboard is blank")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(BugReportUiState.Success("report-1"), viewModel.uiState.value)
        assertEquals("{\"diagnostics\":true}", request.captured.diagnosticsMetadata)
        assertEquals("crash-1", request.captured.crashReportId)
        coVerify(exactly = 1) { repository.enqueue(any(), null) }
    }

    @Test
    fun rapidSubmitTapsEnqueueOnlyOnce() = runTest {
        coEvery { repository.enqueue(any(), null) } returns Result.success(
            QueueEnqueueResult("report-rapid", alreadyQueued = false)
        )

        viewModel.onDescriptionChanged("The dashboard is blank")
        viewModel.submit()
        viewModel.submit()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.enqueue(any(), null) }
    }

    @Test
    fun submit_withScreenshot_isPersistedBeforeScheduling() = runTest {
        val screenshot = ScreenshotAttachment("content://image", byteArrayOf(1, 2), "image/png")
        coEvery { reader.read("content://image") } returns Result.success(screenshot)
        coEvery { repository.enqueue(any(), screenshot) } returns Result.success(
            QueueEnqueueResult("report-2", alreadyQueued = false)
        )

        viewModel.onScreenshotSelected("content://image")
        advanceUntilIdle()
        viewModel.onDescriptionChanged("The image is missing")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(BugReportUiState.Success("report-2"), viewModel.uiState.value)
        coVerify(exactly = 1) { repository.enqueue(any(), screenshot) }
        coVerify(exactly = 1) { scheduler.enqueuePendingUploads() }
    }

    @Test
    fun queueFailure_showsMeaningfulFailure() = runTest {
        val screenshot = ScreenshotAttachment("content://image", byteArrayOf(1), "image/jpeg")
        coEvery { reader.read("content://image") } returns Result.success(screenshot)
        coEvery { repository.enqueue(any(), screenshot) } returns
            Result.failure(IllegalStateException("storage unavailable"))

        viewModel.onScreenshotSelected("content://image")
        advanceUntilIdle()
        viewModel.onDescriptionChanged("The upload fails")
        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is BugReportUiState.Failure)
        assertEquals(
            "The report could not be submitted. Please try again.",
            (state as BugReportUiState.Failure).message
        )
    }

    @Test
    fun submit_blankDescription_returnsValidationErrorWithoutNetworkCall() = runTest {
        viewModel.submit()

        assertTrue(viewModel.uiState.value is BugReportUiState.ValidationError)
        coVerify(exactly = 0) { repository.enqueue(any(), any()) }
    }

    @Test
    fun selectingUnsupportedScreenshot_returnsValidationError() = runTest {
        val screenshot = ScreenshotAttachment("content://image", byteArrayOf(1), "image/gif")
        coEvery { reader.read("content://image") } returns Result.success(screenshot)

        viewModel.onScreenshotSelected("content://image")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is BugReportUiState.ValidationError)
        assertEquals(
            "Screenshot must be a JPEG, PNG, or WEBP image.",
            (state as BugReportUiState.ValidationError).message
        )
    }

    private fun diagnosticsBundle() = DiagnosticsBundle(
        formatVersion = 1,
        exportedAt = 123L,
        deviceInfo = DeviceInfo(
            appVersion = "1.0.0",
            versionCode = 1,
            buildType = "debug",
            androidVersion = "15",
            sdkInt = 35,
            manufacturer = "Google",
            model = "Pixel",
            brand = "google",
            abi = "arm64-v8a",
            locale = "en-US",
            densityDpi = 420,
            memoryClassMb = 256
        ),
        crashReports = listOf(
            CrashReport(
                id = "crash-1",
                timestamp = 10L,
                threadName = "main",
                exceptionClass = "IllegalStateException",
                message = "failure",
                stackTrace = listOf("stack"),
                appVersion = "1.0.0",
                versionCode = 1,
                buildType = "debug",
                deviceInfo = DeviceInfo(
                    appVersion = "1.0.0",
                    versionCode = 1,
                    buildType = "debug",
                    androidVersion = "15",
                    sdkInt = 35,
                    manufacturer = "Google",
                    model = "Pixel",
                    brand = "google",
                    abi = "arm64-v8a",
                    locale = "en-US",
                    densityDpi = 420,
                    memoryClassMb = 256
                ),
                recentLogs = emptyList()
            )
        ),
        logs = emptyList()
    )
}
