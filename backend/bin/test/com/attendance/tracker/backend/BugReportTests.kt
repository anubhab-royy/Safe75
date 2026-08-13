package com.attendance.tracker.backend

import com.attendance.tracker.backend.feature.reports.model.*
import com.attendance.tracker.backend.feature.reports.routes.bugReportRoutes
import com.attendance.tracker.backend.feature.reports.service.BugReportService
import com.attendance.tracker.backend.feature.reports.validation.BugReportValidator
import com.attendance.tracker.backend.feature.reports.validation.ValidationException
import com.attendance.tracker.backend.feature.reports.repository.BugReportRepository
import com.attendance.tracker.backend.security.repository.DeviceRepository
import com.attendance.tracker.backend.security.model.Device
import com.attendance.tracker.backend.security.model.DeviceStatus
import com.attendance.tracker.backend.security.calculateHmac
import com.attendance.tracker.backend.storage.R2StorageService
import com.attendance.tracker.backend.util.Uuid7Generator
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.routing
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.serialization.json.*
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.test.*

class BugReportTests {

    @Test
    fun testUuid7GeneratorOrdering() {
        val u1 = Uuid7Generator.generate()
        Thread.sleep(2) // ensure timestamp moves slightly
        val u2 = Uuid7Generator.generate()
        assertTrue(u1 < u2, "UUIDv7 should be lexicographically time-ordered")
    }

    @Test
    fun testBugReportValidator() {
        val validRequest = BugReportCreateRequest(
            timestamp = System.currentTimeMillis(),
            appVersion = "1.2.3",
            versionCode = 10,
            buildType = "release",
            androidVersion = "13",
            sdkVersion = 33,
            deviceManufacturer = "Google",
            deviceModel = "Pixel 7",
            cpuAbi = "arm64-v8a",
            locale = "en-US",
            userDescription = "A valid description.",
            diagnosticsMetadata = "{}"
        )
        // Should succeed
        BugReportValidator.validate(validRequest)

        // Invalid App Version
        val invalidVersion = validRequest.copy(appVersion = "invalid-version")
        assertFailsWith<ValidationException> {
            BugReportValidator.validate(invalidVersion)
        }

        // Long Description
        val longDesc = validRequest.copy(userDescription = "A".repeat(1001))
        assertFailsWith<ValidationException> {
            BugReportValidator.validate(longDesc)
        }

        // Future Timestamp
        val futureTime = validRequest.copy(timestamp = System.currentTimeMillis() + 86400000L) // 1 day future
        assertFailsWith<ValidationException> {
            BugReportValidator.validate(futureTime)
        }
    }

    @Test
    fun testPostReportRouteSuccess() = testApplication {
        val mockService = mockk<BugReportService>()
        val mockDeviceRepo = mockk<DeviceRepository>()
        val mockReport = BugReport(
            reportId = "018db2c3-4d56-7f8a-9b0c-1d2e3f4a5b6c",
            timestamp = 1723000000000L,
            appVersion = "1.0.0",
            versionCode = 1,
            buildType = "debug",
            androidVersion = "14",
            sdkVersion = 34,
            deviceManufacturer = "Samsung",
            deviceModel = "Galaxy S23",
            cpuAbi = "arm64-v8a",
            locale = "en_US",
            crashReportId = null,
            userDescription = "Test description",
            diagnosticsMetadata = "{}",
            uploadStatus = UploadStatus.PENDING,
            retryCount = 0,
            screenshotObjectKey = null,
            createdAt = 1723000005000L,
            updatedAt = 1723000005000L
        )

        val deviceId = "dev_123"
        val secret = "sec_abc"
        coEvery { mockService.createReport(any()) } returns mockReport
        coEvery { mockDeviceRepo.findById(deviceId) } returns Device(deviceId, secret, DeviceStatus.ACTIVE, 0L, 0L)
        coEvery { mockDeviceRepo.updateLastSeen(deviceId) } returns true

        application {
            install(DoubleReceive)
            install(ContentNegotiation) {
                json()
            }
            routing {
                bugReportRoutes(mockService, mockDeviceRepo)
            }
        }

        val requestBody = """
            {
                "timestamp": 1723000000000,
                "appVersion": "1.0.0",
                "versionCode": 1,
                "buildType": "debug",
                "androidVersion": "14",
                "sdkVersion": 34,
                "deviceManufacturer": "Samsung",
                "deviceModel": "Galaxy S23",
                "cpuAbi": "arm64-v8a",
                "locale": "en_US",
                "userDescription": "Test description",
                "diagnosticsMetadata": "{}"
            }
        """.trimIndent()

        val nonce = "nonce_success"
        val timestamp = System.currentTimeMillis()
        val sig = calculateHmac("$requestBody$nonce$timestamp", secret)

        val response = client.post("/api/v1/reports") {
            contentType(ContentType.Application.Json)
            header("X-Safe75-DeviceId", deviceId)
            header("X-Safe75-Signature", sig)
            header("X-Safe75-Nonce", nonce)
            header("X-Safe75-Timestamp", timestamp.toString())
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = response.bodyAsText()
        val json = Json.parseToJsonElement(responseBody).jsonObject
        assertEquals("018db2c3-4d56-7f8a-9b0c-1d2e3f4a5b6c", json["reportId"]?.jsonPrimitive?.content)
        assertEquals("PENDING", json["status"]?.jsonPrimitive?.content)
    }

    @Test
    fun testGetReportRouteNotFound() = testApplication {
        val mockService = mockk<BugReportService>()
        val mockDeviceRepo = mockk<DeviceRepository>()
        coEvery { mockService.getReport(any()) } returns null

        val deviceId = "dev_123"
        val secret = "sec_abc"
        coEvery { mockDeviceRepo.findById(deviceId) } returns Device(deviceId, secret, DeviceStatus.ACTIVE, 0L, 0L)
        coEvery { mockDeviceRepo.updateLastSeen(deviceId) } returns true

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                bugReportRoutes(mockService, mockDeviceRepo)
            }
        }

        val nonce = "nonce_not_found"
        val timestamp = System.currentTimeMillis()
        val expectedSig = calculateHmac("$nonce$timestamp", secret) // GET has empty body

        val response = client.get("/api/v1/reports/non-existent-id") {
            header("X-Safe75-DeviceId", deviceId)
            header("X-Safe75-Signature", expectedSig)
            header("X-Safe75-Nonce", nonce)
            header("X-Safe75-Timestamp", timestamp.toString())
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun testScreenshotUploadSuccess() = runTest {
        val mockRepo = mockk<BugReportRepository>()
        val mockR2 = mockk<R2StorageService>()
        val service = BugReportService(mockRepo, mockR2)

        val reportId = "018db2c3-4d56-7f8a-9b0c-1d2e3f4a5b6c"
        val mockReport = BugReport(
            reportId = reportId,
            timestamp = 1723000000000L,
            appVersion = "1.0.0",
            versionCode = 1,
            buildType = "debug",
            androidVersion = "14",
            sdkVersion = 34,
            deviceManufacturer = "Samsung",
            deviceModel = "Galaxy S23",
            cpuAbi = "arm64-v8a",
            locale = "en_US",
            crashReportId = null,
            userDescription = "Test",
            diagnosticsMetadata = "{}",
            uploadStatus = UploadStatus.PENDING,
            retryCount = 0,
            screenshotObjectKey = null,
            createdAt = 1723000005000L,
            updatedAt = 1723000005000L
        )

        val bos = ByteArrayOutputStream()
        val bi = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
        ImageIO.write(bi, "png", bos)
        val validPngBytes = bos.toByteArray()

        coEvery { mockRepo.findById(reportId) } returns mockReport
        every { mockR2.upload(any(), any(), any()) } just runs
        coEvery { mockRepo.updateScreenshot(reportId, any(), UploadStatus.SCREENSHOT_UPLOADED) } returns true

        val key = service.uploadScreenshot(reportId, validPngBytes)
        assertEquals("reports/$reportId/screenshot.jpg", key)

        verify(exactly = 1) { mockR2.upload("reports/$reportId/screenshot.jpg", any(), "image/jpeg") }
        coVerify(exactly = 1) { mockRepo.updateScreenshot(reportId, "reports/$reportId/screenshot.jpg", UploadStatus.SCREENSHOT_UPLOADED) }
    }

    @Test
    fun testScreenshotUploadOversized() = runTest {
        val mockRepo = mockk<BugReportRepository>()
        val mockR2 = mockk<R2StorageService>()
        val service = BugReportService(mockRepo, mockR2)
        val reportId = "018db2c3-4d56-7f8a-9b0c-1d2e3f4a5b6c"

        val mockReport = mockk<BugReport>()
        every { mockReport.screenshotObjectKey } returns null
        coEvery { mockRepo.findById(reportId) } returns mockReport

        val oversizedBytes = ByteArray(2 * 1024 * 1024 + 1) // 2MB + 1 byte

        assertFailsWith<IllegalArgumentException> {
            service.uploadScreenshot(reportId, oversizedBytes)
        }
    }

    @Test
    fun testScreenshotUploadInvalidFormat() = runTest {
        val mockRepo = mockk<BugReportRepository>()
        val mockR2 = mockk<R2StorageService>()
        val service = BugReportService(mockRepo, mockR2)
        val reportId = "018db2c3-4d56-7f8a-9b0c-1d2e3f4a5b6c"

        val mockReport = mockk<BugReport>()
        every { mockReport.screenshotObjectKey } returns null
        coEvery { mockRepo.findById(reportId) } returns mockReport

        val invalidBytes = "This is a text file disguised as a screenshot".toByteArray()

        assertFailsWith<IllegalArgumentException> {
            service.uploadScreenshot(reportId, invalidBytes)
        }
    }

    @Test
    fun testScreenshotUploadRollbackOnDbFailure() = runTest {
        val mockRepo = mockk<BugReportRepository>()
        val mockR2 = mockk<R2StorageService>()
        val service = BugReportService(mockRepo, mockR2)

        val reportId = "018db2c3-4d56-7f8a-9b0c-1d2e3f4a5b6c"
        val mockReport = BugReport(
            reportId = reportId,
            timestamp = 1723000000000L,
            appVersion = "1.0.0",
            versionCode = 1,
            buildType = "debug",
            androidVersion = "14",
            sdkVersion = 34,
            deviceManufacturer = "Samsung",
            deviceModel = "Galaxy S23",
            cpuAbi = "arm64-v8a",
            locale = "en_US",
            crashReportId = null,
            userDescription = "Test",
            diagnosticsMetadata = "{}",
            uploadStatus = UploadStatus.PENDING,
            retryCount = 0,
            screenshotObjectKey = null,
            createdAt = 1723000005000L,
            updatedAt = 1723000005000L
        )

        val bos = ByteArrayOutputStream()
        val bi = BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB)
        ImageIO.write(bi, "png", bos)
        val validPngBytes = bos.toByteArray()

        coEvery { mockRepo.findById(reportId) } returns mockReport
        every { mockR2.upload(any(), any(), any()) } just runs
        every { mockR2.delete(any()) } just runs
        coEvery { mockRepo.updateScreenshot(reportId, any(), any()) } returns false // database write fails!

        assertFailsWith<RuntimeException> {
            service.uploadScreenshot(reportId, validPngBytes)
        }

        // Verify that delete was called to rollback the file upload!
        verify(exactly = 1) { mockR2.delete("reports/$reportId/screenshot.jpg") }
    }

    @Test
    fun testScreenshotUploadDuplicateFails() = runTest {
        val mockRepo = mockk<BugReportRepository>()
        val mockR2 = mockk<R2StorageService>()
        val service = BugReportService(mockRepo, mockR2)

        val reportId = "018db2c3-4d56-7f8a-9b0c-1d2e3f4a5b6c"
        val mockReport = BugReport(
            reportId = reportId,
            timestamp = 1723000000000L,
            appVersion = "1.0.0",
            versionCode = 1,
            buildType = "debug",
            androidVersion = "14",
            sdkVersion = 34,
            deviceManufacturer = "Samsung",
            deviceModel = "Galaxy S23",
            cpuAbi = "arm64-v8a",
            locale = "en_US",
            crashReportId = null,
            userDescription = "Test",
            diagnosticsMetadata = "{}",
            uploadStatus = UploadStatus.SCREENSHOT_UPLOADED,
            retryCount = 0,
            screenshotObjectKey = "reports/$reportId/screenshot.jpg", // already set!
            createdAt = 1723000005000L,
            updatedAt = 1723000005000L
        )

        coEvery { mockRepo.findById(reportId) } returns mockReport

        val validPngBytes = ByteArray(10)

        assertFailsWith<IllegalStateException> {
            service.uploadScreenshot(reportId, validPngBytes)
        }
    }
}
