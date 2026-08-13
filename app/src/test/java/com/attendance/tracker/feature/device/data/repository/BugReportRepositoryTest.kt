package com.attendance.tracker.feature.device.data.repository

import com.attendance.tracker.feature.device.data.model.BugReportCreateResponse
import com.attendance.tracker.feature.device.data.model.ScreenshotUploadResponse
import com.attendance.tracker.feature.device.data.remote.Safe75ApiService
import com.attendance.tracker.feature.device.data.security.HmacSigner
import com.attendance.tracker.feature.device.data.security.PoWSolver
import com.attendance.tracker.feature.device.data.security.SecureDeviceStorage
import com.attendance.tracker.feature.device.domain.model.BugReportSubmission
import com.attendance.tracker.feature.device.domain.model.EnrolledDevice
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class BugReportRepositoryTest {
    private val apiService = mockk<Safe75ApiService>()
    private val secureStorage = mockk<SecureDeviceStorage>(relaxed = true)
    private lateinit var repository: BackendRepositoryImpl

    @Before
    fun setUp() {
        repository = BackendRepositoryImpl(apiService, secureStorage, PoWSolver, HmacSigner)
    }

    @Test
    fun submitBugReport_returnsBackendReportId() = runTest {
        coEvery { secureStorage.getDevice() } returns EnrolledDevice("device", "secret", 1L)
        coEvery { apiService.submitBugReport(any()) } returns Response.success(
            BugReportCreateResponse("report-1", "PENDING")
        )

        val result = repository.submitBugReport(submission())

        assertEquals("report-1", result.getOrThrow())
        coVerify(exactly = 1) { apiService.submitBugReport(any()) }
    }

    @Test
    fun submitBugReport_withoutEnrollment_doesNotCallBackend() = runTest {
        coEvery { secureStorage.getDevice() } returns null

        val result = repository.submitBugReport(submission())

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { apiService.submitBugReport(any()) }
    }

    @Test
    fun uploadScreenshot_sendsMultipartAndSucceeds() = runTest {
        coEvery { secureStorage.getDevice() } returns EnrolledDevice("device", "secret", 1L)
        coEvery { apiService.uploadScreenshot(any(), any()) } returns Response.success(
            ScreenshotUploadResponse("report-1", "SCREENSHOT_UPLOADED", "reports/report-1/screenshot.jpg")
        )

        val result = repository.uploadScreenshot("report-1", byteArrayOf(1, 2, 3), "image/png")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { apiService.uploadScreenshot("report-1", any()) }
    }

    @Test
    fun uploadScreenshot_backendFailure_returnsFailure() = runTest {
        coEvery { secureStorage.getDevice() } returns EnrolledDevice("device", "secret", 1L)
        coEvery { apiService.uploadScreenshot(any(), any()) } returns Response.error(
            400,
            "{\"error\":\"invalid image\"}".toResponseBody()
        )

        val result = repository.uploadScreenshot("report-1", byteArrayOf(1), "image/png")

        assertTrue(result.isFailure)
    }

    private fun submission() = BugReportSubmission(
        timestamp = 100L,
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
        userDescription = "The screen does not load",
        diagnosticsMetadata = "{}"
    )
}
