package com.attendance.tracker.feature.bugreport.data

import com.attendance.tracker.core.common.DispatcherProvider
import com.attendance.tracker.data.local.database.dao.BugReportQueueDao
import com.attendance.tracker.data.local.database.entity.BugReportQueueEntity
import com.attendance.tracker.feature.bugreport.domain.model.BugReportQueueStatus
import com.attendance.tracker.feature.bugreport.model.ScreenshotAttachment
import com.attendance.tracker.feature.device.domain.model.BugReportSubmission
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BugReportQueueRepositoryImplTest {
    private val dao = mockk<BugReportQueueDao>()
    private val attachmentStore = mockk<BugReportAttachmentStore>(relaxed = true)
    private lateinit var repository: BugReportQueueRepositoryImpl

    @Before
    fun setUp() {
        repository = BugReportQueueRepositoryImpl(
            dao,
            attachmentStore,
            object : DispatcherProvider {
                override val main = Dispatchers.Unconfined
                override val io = Dispatchers.Unconfined
                override val default = Dispatchers.Unconfined
            }
        )
    }

    @Test
    fun enqueueStoresReportAndScreenshot() = runTest {
        coEvery { dao.findByFingerprint(any()) } returns null
        coEvery { attachmentStore.write(any(), any()) } returns "/private/local.image"
        coEvery { dao.insert(any()) } returns 1L

        val result = repository.enqueue(submission(), screenshot())

        assertTrue(result.isSuccess)
        assertTrue(!result.getOrThrow().alreadyQueued)
        coVerify { attachmentStore.write(any(), byteArrayOf(1, 2, 3)) }
        coVerify { dao.insert(match { it.status == BugReportQueueStatus.QUEUED.name }) }
    }

    @Test
    fun enqueueSameFingerprintReturnsExistingReportWithoutWritingFile() = runTest {
        coEvery { dao.findByFingerprint(any()) } returns entity()

        val result = repository.enqueue(submission(), screenshot())

        assertEquals("local-existing", result.getOrThrow().reportId)
        assertTrue(result.getOrThrow().alreadyQueued)
        coVerify(exactly = 0) { attachmentStore.write(any(), any()) }
        coVerify(exactly = 0) { dao.insert(any()) }
    }

    @Test
    fun claimNextAtomicallyClaimsQueuedMetadata() = runTest {
        val queued = entity(status = BugReportQueueStatus.QUEUED.name)
        val claimed = queued.copy(status = BugReportQueueStatus.UPLOADING_METADATA.name)
        coEvery { dao.findNextPending(any()) } returns queued
        coEvery { dao.claimMetadata("local-existing", any()) } returns 1
        coEvery { dao.findById("local-existing") } returns claimed

        val result = repository.claimNext()

        assertEquals(BugReportQueueStatus.UPLOADING_METADATA, result?.status)
        assertEquals("local-existing", result?.reportId)
    }

    private fun screenshot() = ScreenshotAttachment(
        uri = "content://image",
        bytes = byteArrayOf(1, 2, 3),
        contentType = "image/png"
    )

    private fun submission() = BugReportSubmission(
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
    )

    private fun entity(status: String = BugReportQueueStatus.QUEUED.name) = BugReportQueueEntity(
        reportId = "local-existing",
        fingerprint = "fingerprint",
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
        diagnosticsMetadata = "{}",
        screenshotPath = null,
        screenshotContentType = null,
        remoteReportId = null,
        status = status,
        retryCount = 0,
        createdAt = 1L,
        updatedAt = 1L,
        lastError = null
    )
}
