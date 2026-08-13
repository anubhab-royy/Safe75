package com.attendance.tracker.feature.bugreport.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.attendance.tracker.feature.bugreport.data.BugReportAttachmentStore
import com.attendance.tracker.feature.bugreport.data.BugReportRetryPolicy
import com.attendance.tracker.feature.bugreport.domain.model.BugReportQueueStatus
import com.attendance.tracker.feature.bugreport.domain.model.QueuedBugReport
import com.attendance.tracker.feature.bugreport.domain.repository.BugReportQueueRepository
import com.attendance.tracker.feature.device.data.remote.NetworkError
import com.attendance.tracker.feature.device.domain.model.BugReportSubmission
import com.attendance.tracker.feature.device.domain.repository.BackendRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BugReportUploadWorkerTest {
    private val queue = mockk<BugReportQueueRepository>(relaxed = true)
    private val backend = mockk<BackendRepository>()
    private val attachments = mockk<BugReportAttachmentStore>(relaxed = true)
    private lateinit var worker: BugReportUploadWorker

    @Before
    fun setUp() {
        coEvery { backend.getEnrolledDevice() } returns mockk(relaxed = true)
        worker = BugReportUploadWorker(
            mockk<Context>(relaxed = true),
            mockk<WorkerParameters>(relaxed = true),
            queue,
            backend,
            attachments
        )
    }

    @Test
    fun metadataSuccessWithoutScreenshotDeletesQueueEntry() = runTest {
        coEvery { queue.claimNext() } returnsMany listOf(report(), null)
        coEvery { backend.submitBugReport(any()) } returns Result.success("remote-1")

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        coVerify { queue.markMetadataUploaded("local-1", "remote-1") }
        coVerify { queue.delete("local-1") }
    }

    @Test
    fun transientMetadataFailureRequestsWorkManagerRetry() = runTest {
        coEvery { queue.claimNext() } returns report()
        coEvery { backend.submitBugReport(any()) } returns Result.failure(NetworkError.Timeout())
        coEvery { queue.markRetry(any(), any()) } returns 1

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
        coVerify { queue.markRetry("local-1", "Request timed out") }
    }

    @Test
    fun validationFailureDoesNotRetry() = runTest {
        coEvery { queue.claimNext() } returnsMany listOf(report(), null)
        coEvery { backend.submitBugReport(any()) } returns Result.failure(NetworkError.Validation())

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        coVerify { queue.markPermanentFailure("local-1", "Backend validation failed") }
    }

    @Test
    fun partialUploadResumesAtScreenshotOnly() = runTest {
        val metadata = report()
        val screenshot = metadata.copy(
            remoteReportId = "remote-2",
            status = BugReportQueueStatus.SCREENSHOT_PENDING,
            screenshotPath = "/private/report.image",
            screenshotContentType = "image/png"
        )
        coEvery { queue.claimNext() } returnsMany listOf(metadata, screenshot, null)
        coEvery { backend.submitBugReport(any()) } returns Result.success("remote-2")
        val pngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        coEvery { attachments.read("/private/report.image") } returns pngBytes
        coEvery { backend.uploadScreenshot("remote-2", pngBytes, "image/png") } returns Result.success(Unit)

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        coVerify(exactly = 1) { backend.submitBugReport(any()) }
        coVerify(exactly = 1) { backend.uploadScreenshot("remote-2", pngBytes, "image/png") }
        coVerify { queue.delete("local-1") }
    }

    @Test
    fun missingScreenshotIsPermanentAndNotRetried() = runTest {
        val screenshot = report().copy(
            remoteReportId = "remote-3",
            status = BugReportQueueStatus.SCREENSHOT_PENDING,
            screenshotPath = "/missing.image",
            screenshotContentType = "image/png"
        )
        coEvery { queue.claimNext() } returnsMany listOf(screenshot, null)
        coEvery { attachments.read("/missing.image") } returns null

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        coVerify { queue.markPermanentFailure("local-1", "Screenshot file is missing or invalid") }
        coVerify(exactly = 0) { backend.uploadScreenshot(any(), any(), any()) }
    }

    @Test
    fun invalidScreenshotSignatureReturnsFailureWithoutNetworkCall() = runTest {
        val screenshot = report().copy(
            remoteReportId = "remote-4",
            status = BugReportQueueStatus.SCREENSHOT_PENDING,
            screenshotPath = "/invalid.image",
            screenshotContentType = "image/png"
        )
        coEvery { queue.claimNext() } returnsMany listOf(screenshot, null)
        coEvery { attachments.read("/invalid.image") } returns byteArrayOf(1, 2, 3)

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        coVerify { queue.markPermanentFailure("local-1", "Screenshot file is invalid") }
        coVerify(exactly = 0) { backend.uploadScreenshot(any(), any(), any()) }
    }

    private fun report() = QueuedBugReport(
        reportId = "local-1",
        submission = BugReportSubmission(
            timestamp = 1L,
            appVersion = "1.0.0",
            versionCode = 1,
            buildType = "debug",
            androidVersion = "15",
            sdkVersion = 35,
            deviceManufacturer = "Google",
            deviceModel = "Pixel",
            cpuAbi = "arm64-v8a",
            locale = "en-US",
            crashReportId = null,
            userDescription = "The dashboard is blank",
            diagnosticsMetadata = "{}"
        ),
        screenshotPath = null,
        screenshotContentType = null,
        remoteReportId = null,
        status = BugReportQueueStatus.UPLOADING_METADATA,
        retryCount = 0,
        createdAt = 1L,
        updatedAt = 1L,
        lastError = null
    )
}
