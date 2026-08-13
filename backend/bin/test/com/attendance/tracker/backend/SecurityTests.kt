package com.attendance.tracker.backend

import com.attendance.tracker.backend.security.*
import com.attendance.tracker.backend.security.model.*
import com.attendance.tracker.backend.security.repository.DeviceRepository
import com.attendance.tracker.backend.security.routes.securityRoutes
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.serialization.json.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class SecurityTests {

    @BeforeTest
    fun setup() {
        RateLimiter.reset()
        NonceCache.reset()
    }

    @Test
    fun testProofOfWorkVerification() {
        val seed = ChallengeManager.generateChallenge()
        val difficulty = ChallengeManager.difficulty

        // Solve PoW puzzle
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

        assertTrue(ChallengeManager.verifyChallenge(seed, nonce), "Proof of work solution should be valid")
    }

    @Test
    fun testChallengeAndEnrollRoutes() = testApplication {
        val mockRepo = mockk<DeviceRepository>()
        coEvery { mockRepo.save(any()) } returns true

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                securityRoutes(mockRepo)
            }
        }

        // 1. Fetch PoW challenge seed
        val challengeResponse = client.get("/api/v1/device/challenge")
        assertEquals(HttpStatusCode.OK, challengeResponse.status)
        val body = challengeResponse.bodyAsText()
        val challengeJson = Json.parseToJsonElement(body).jsonObject
        val seed = challengeJson["seed"]?.jsonPrimitive?.content ?: ""
        val difficulty = challengeJson["difficulty"]?.jsonPrimitive?.int ?: 0

        assertTrue(seed.isNotEmpty())
        assertEquals(ChallengeManager.difficulty, difficulty)

        // 2. Solve challenge
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

        // 3. Enroll device using raw JSON string payload
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
        val enrollBody = enrollResponse.bodyAsText()
        val enrollJson = Json.parseToJsonElement(enrollBody).jsonObject
        val deviceId = enrollJson["deviceId"]?.jsonPrimitive?.content ?: ""
        val deviceSecret = enrollJson["deviceSecret"]?.jsonPrimitive?.content ?: ""

        assertTrue(deviceId.startsWith("dev_"))
        assertTrue(deviceSecret.startsWith("sec_"))
    }

    @Test
    fun testAuthenticationMiddlewareSuccess() = testApplication {
        val mockRepo = mockk<DeviceRepository>()
        val deviceId = "dev_123"
        val secret = "sec_abc"
        val mockDevice = Device(
            deviceId = deviceId,
            deviceSecret = secret,
            status = DeviceStatus.ACTIVE,
            createdAt = 1723000000000L,
            lastSeen = 1723000000000L
        )

        coEvery { mockRepo.findById(deviceId) } returns mockDevice
        coEvery { mockRepo.updateLastSeen(deviceId) } returns true

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                install(SecurityAuthenticationPlugin) {
                    deviceRepository = mockRepo
                }
                get("/protected") {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "success"))
                }
            }
        }

        val nonce = "unique_nonce_123"
        val timestamp = System.currentTimeMillis()
        val expectedSig = calculateHmac("$nonce$timestamp", secret) // GET has empty body

        val response = client.get("/protected") {
            header("X-Safe75-DeviceId", deviceId)
            header("X-Safe75-Signature", expectedSig)
            header("X-Safe75-Nonce", nonce)
            header("X-Safe75-Timestamp", timestamp.toString())
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun testAuthenticationMiddlewareExpiredTimestamp() = testApplication {
        val mockRepo = mockk<DeviceRepository>()
        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                install(SecurityAuthenticationPlugin) {
                    deviceRepository = mockRepo
                }
                get("/protected") {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "success"))
                }
            }
        }

        val expiredTimestamp = System.currentTimeMillis() - 600000L // 10 minutes ago
        val response = client.get("/protected") {
            header("X-Safe75-DeviceId", "dev_123")
            header("X-Safe75-Signature", "sig")
            header("X-Safe75-Nonce", "nonce")
            header("X-Safe75-Timestamp", expiredTimestamp.toString())
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testAuthenticationMiddlewareReplayNonce() = testApplication {
        val mockRepo = mockk<DeviceRepository>()
        val deviceId = "dev_123"
        val secret = "sec_abc"
        val mockDevice = Device(deviceId, secret, DeviceStatus.ACTIVE, 0L, 0L)

        coEvery { mockRepo.findById(deviceId) } returns mockDevice
        coEvery { mockRepo.updateLastSeen(deviceId) } returns true

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                install(SecurityAuthenticationPlugin) {
                    deviceRepository = mockRepo
                }
                get("/protected") {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "success"))
                }
            }
        }

        val nonce = "replay_nonce_1"
        val timestamp = System.currentTimeMillis()
        val sig = calculateHmac("$nonce$timestamp", secret)

        // First attempt: OK
        val res1 = client.get("/protected") {
            header("X-Safe75-DeviceId", deviceId)
            header("X-Safe75-Signature", sig)
            header("X-Safe75-Nonce", nonce)
            header("X-Safe75-Timestamp", timestamp.toString())
        }
        assertEquals(HttpStatusCode.OK, res1.status)

        // Second attempt with same nonce: 401 Unauthorized
        val res2 = client.get("/protected") {
            header("X-Safe75-DeviceId", deviceId)
            header("X-Safe75-Signature", sig)
            header("X-Safe75-Nonce", nonce)
            header("X-Safe75-Timestamp", timestamp.toString())
        }
        assertEquals(HttpStatusCode.Unauthorized, res2.status)
    }

    @Test
    fun testAuthenticationMiddlewareRateLimiter() = testApplication {
        val mockRepo = mockk<DeviceRepository>()
        coEvery { mockRepo.findById("dev_rate") } returns null

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                install(SecurityAuthenticationPlugin) {
                    deviceRepository = mockRepo
                }
                get("/protected") {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "success"))
                }
            }
        }

        // Trigger rate limiter by exceeding IP limit (default max is 30, execute 35 calls)
        var triggered429 = false
        for (i in 1..35) {
            val response = client.get("/protected") {
                header("X-Safe75-DeviceId", "dev_rate")
                header("X-Safe75-Signature", "sig")
                header("X-Safe75-Nonce", "nonce_$i")
                header("X-Safe75-Timestamp", System.currentTimeMillis().toString())
            }
            if (response.status == HttpStatusCode.TooManyRequests) {
                triggered429 = true
                break
            }
        }

        assertTrue(triggered429, "Rate limit should be triggered returning 429")
    }

    @Test
    fun testAuthenticationMiddlewareBlockedDevice() = testApplication {
        val mockRepo = mockk<DeviceRepository>()
        val deviceId = "dev_blocked"
        val mockDevice = Device(deviceId, "sec", DeviceStatus.BLOCKED, 0L, 0L)

        coEvery { mockRepo.findById(deviceId) } returns mockDevice

        application {
            install(ContentNegotiation) {
                json()
            }
            routing {
                install(SecurityAuthenticationPlugin) {
                    deviceRepository = mockRepo
                }
                get("/protected") {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "success"))
                }
            }
        }

        val response = client.get("/protected") {
            header("X-Safe75-DeviceId", deviceId)
            header("X-Safe75-Signature", "sig")
            header("X-Safe75-Nonce", "nonce")
            header("X-Safe75-Timestamp", System.currentTimeMillis().toString())
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }
}
