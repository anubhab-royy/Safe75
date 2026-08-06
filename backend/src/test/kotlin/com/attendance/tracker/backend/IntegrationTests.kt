package com.attendance.tracker.backend

import com.attendance.tracker.backend.feature.reports.model.*
import com.attendance.tracker.backend.feature.reports.repository.BugReportRepository
import com.attendance.tracker.backend.feature.reports.routes.bugReportRoutes
import com.attendance.tracker.backend.feature.reports.service.BugReportService
import com.attendance.tracker.backend.security.*
import com.attendance.tracker.backend.security.model.*
import com.attendance.tracker.backend.security.repository.DeviceRepository
import com.attendance.tracker.backend.security.routes.securityRoutes
import com.attendance.tracker.backend.storage.R2StorageService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.routing.routing
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.test.*

class IntegrationTests {

    @BeforeTest
    fun setup() {
        RateLimiter.reset()
        NonceCache.reset()
    }

    @Test
    fun testCompleteFlowAndLoadSimulation() = testApplication {
        val mockDeviceRepo = mockk<DeviceRepository>()
        val mockReportRepo = mockk<BugReportRepository>()
        val mockR2Storage = mockk<R2StorageService>()

        val deviceMap = mutableMapOf<String, Device>()
        val reportMap = mutableMapOf<String, BugReport>()

        coEvery { mockDeviceRepo.save(any()) } answers {
            val dev = firstArg<Device>()
            deviceMap[dev.deviceId] = dev
            true
        }
        coEvery { mockDeviceRepo.findById(any()) } answers {
            deviceMap[firstArg()]
        }
        coEvery { mockDeviceRepo.updateLastSeen(any()) } returns true

        coEvery { mockReportRepo.save(any()) } answers {
            val rep = firstArg<BugReport>()
            reportMap[rep.reportId] = rep
            true
        }
        coEvery { mockReportRepo.findById(any()) } answers {
            reportMap[firstArg()]
        }
        coEvery { mockReportRepo.updateScreenshot(any(), any(), any()) } answers {
            val id = firstArg<String>()
            val key = secondArg<String?>()
            val status = thirdArg<UploadStatus>()
            val rep = reportMap[id]
            if (rep != null) {
                reportMap[id] = rep.copy(screenshotObjectKey = key, uploadStatus = status, updatedAt = System.currentTimeMillis())
                true
            } else {
                false
            }
        }

        every { mockR2Storage.upload(any(), any(), any()) } just runs

        val bugReportService = BugReportService(mockReportRepo, mockR2Storage)

        application {
            install(DoubleReceive)
            install(ContentNegotiation) {
                json()
            }
            routing {
                securityRoutes(mockDeviceRepo)
                bugReportRoutes(bugReportService, mockDeviceRepo)
            }
        }

        // 1. Fetch PoW challenge seed
        val challengeResponse = client.get("/api/v1/device/challenge")
        assertEquals(HttpStatusCode.OK, challengeResponse.status)
        val chalJson = Json.parseToJsonElement(challengeResponse.bodyAsText()).jsonObject
        val seed = chalJson["seed"]?.jsonPrimitive?.content ?: ""
        val difficulty = chalJson["difficulty"]?.jsonPrimitive?.int ?: 4

        // Solve PoW challenge
        var nonce = 0L
        val prefix = "0".repeat(difficulty)
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        while (true) {
            val input = "$seed$nonce".toByteArray()
            val hash = digest.digest(input).joinToString("") { "%02x".format(it) }
            if (hash.startsWith(prefix)) {
                break
            }
            nonce++
        }

        // 2. Enroll Device
        val enrollResponse = client.post("/api/v1/device/enroll") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "seed": "$seed",
                    "nonce": $nonce
                }
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.Created, enrollResponse.status)
        val enrollJson = Json.parseToJsonElement(enrollResponse.bodyAsText()).jsonObject
        val deviceId = enrollJson["deviceId"]?.jsonPrimitive?.content ?: ""
        val deviceSecret = enrollJson["deviceSecret"]?.jsonPrimitive?.content ?: ""

        assertTrue(deviceId.isNotEmpty())
        assertTrue(deviceSecret.isNotEmpty())

        // 3. Create Bug Report (Signed Request)
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
                "userDescription": "Test integration run",
                "diagnosticsMetadata": "{}"
            }
        """.trimIndent()

        val postNonce = "nonce_post_report"
        val postTimestamp = System.currentTimeMillis()
        val postSig = calculateHmac("$requestBody$postNonce$postTimestamp", deviceSecret)

        val reportResponse = client.post("/api/v1/reports") {
            contentType(ContentType.Application.Json)
            header("X-Safe75-DeviceId", deviceId)
            header("X-Safe75-Signature", postSig)
            header("X-Safe75-Nonce", postNonce)
            header("X-Safe75-Timestamp", postTimestamp.toString())
            setBody(requestBody)
        }
        assertEquals(HttpStatusCode.Created, reportResponse.status)
        val reportJson = Json.parseToJsonElement(reportResponse.bodyAsText()).jsonObject
        val reportId = reportJson["reportId"]?.jsonPrimitive?.content ?: ""
        assertTrue(reportId.isNotEmpty())

        // 4. Upload Screenshot (Signed Request)
        val bos = ByteArrayOutputStream()
        val bi = BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB)
        ImageIO.write(bi, "png", bos)
        val screenshotBytes = bos.toByteArray()

        val boundary = "boundary123"
        val headerPart = ("--$boundary\r\n" +
                "Content-Disposition: form-data; name=\"screenshot\"; filename=\"screenshot.png\"\r\n" +
                "Content-Type: image/png\r\n\r\n").toByteArray(Charsets.UTF_8)
        val footerPart = "\r\n--$boundary--".toByteArray(Charsets.UTF_8)

        val rawMultipartBytes = ByteArray(headerPart.size + screenshotBytes.size + footerPart.size)
        System.arraycopy(headerPart, 0, rawMultipartBytes, 0, headerPart.size)
        System.arraycopy(screenshotBytes, 0, rawMultipartBytes, headerPart.size, screenshotBytes.size)
        System.arraycopy(footerPart, 0, rawMultipartBytes, headerPart.size + screenshotBytes.size, footerPart.size)

        val uploadNonce = "nonce_upload_screenshot"
        val uploadTimestamp = System.currentTimeMillis()
        val rawMultipartString = String(rawMultipartBytes, Charsets.UTF_8)
        val uploadSig = calculateHmac("$rawMultipartString$uploadNonce$uploadTimestamp", deviceSecret)

        val screenshotResponse = client.post("/api/v1/reports/$reportId/screenshot") {
            contentType(ContentType.parse("multipart/form-data; boundary=$boundary"))
            header("X-Safe75-DeviceId", deviceId)
            header("X-Safe75-Signature", uploadSig)
            header("X-Safe75-Nonce", uploadNonce)
            header("X-Safe75-Timestamp", uploadTimestamp.toString())
            setBody(rawMultipartBytes)
        }
        assertEquals(HttpStatusCode.OK, screenshotResponse.status)

        // 5. Retrieve Report & Verify Metadata Links
        val getNonce = "nonce_get_report"
        val getTimestamp = System.currentTimeMillis()
        val getSig = calculateHmac("$getNonce$getTimestamp", deviceSecret)

        val getResponse = client.get("/api/v1/reports/$reportId") {
            header("X-Safe75-DeviceId", deviceId)
            header("X-Safe75-Signature", getSig)
            header("X-Safe75-Nonce", getNonce)
            header("X-Safe75-Timestamp", getTimestamp.toString())
        }
        assertEquals(HttpStatusCode.OK, getResponse.status)
        val retrievedJson = Json.parseToJsonElement(getResponse.bodyAsText()).jsonObject
        assertEquals(reportId, retrievedJson["reportId"]?.jsonPrimitive?.content)
        assertEquals("reports/$reportId/screenshot.jpg", retrievedJson["screenshotObjectKey"]?.jsonPrimitive?.content)
        assertEquals("SCREENSHOT_UPLOADED", retrievedJson["uploadStatus"]?.jsonPrimitive?.content)

        // 6. Concurrent Load Simulation (50 async requests)
        kotlinx.coroutines.coroutineScope {
            val deferreds = (1..50).map { idx ->
                async {
                    val threadNonce = "load_nonce_$idx"
                    val threadTimestamp = System.currentTimeMillis()
                    val threadSig = calculateHmac("$threadNonce$threadTimestamp", deviceSecret)

                    client.get("/api/v1/reports/$reportId") {
                        header("X-Safe75-DeviceId", deviceId)
                        header("X-Safe75-Signature", threadSig)
                        header("X-Safe75-Nonce", threadNonce)
                        header("X-Safe75-Timestamp", threadTimestamp.toString())
                    }
                }
            }

            val responses = deferreds.awaitAll()
            responses.forEach { resp ->
                assertTrue(resp.status == HttpStatusCode.OK || resp.status == HttpStatusCode.TooManyRequests)
            }
        }
    }
}
