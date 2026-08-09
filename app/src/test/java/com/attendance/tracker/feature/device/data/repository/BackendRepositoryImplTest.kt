package com.attendance.tracker.feature.device.data.repository

import com.attendance.tracker.feature.device.data.model.ChallengeResponse
import com.attendance.tracker.feature.device.data.model.EnrollResponse
import com.attendance.tracker.feature.device.data.model.HealthResponse
import com.attendance.tracker.feature.device.data.remote.NetworkError
import com.attendance.tracker.feature.device.data.remote.Safe75ApiService
import com.attendance.tracker.feature.device.data.security.HmacSigner
import com.attendance.tracker.feature.device.data.security.PoWSolver
import com.attendance.tracker.feature.device.data.security.SecureDeviceStorage
import com.attendance.tracker.feature.device.domain.model.EnrolledDevice
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class BackendRepositoryImplTest {

    private lateinit var apiService: Safe75ApiService
    private lateinit var secureStorage: SecureDeviceStorage
    private lateinit var poWSolver: PoWSolver
    private lateinit var hmacSigner: HmacSigner
    private lateinit var repository: BackendRepositoryImpl

    @Before
    fun setUp() {
        apiService = mockk()
        secureStorage = mockk(relaxed = true)
        poWSolver = PoWSolver
        hmacSigner = HmacSigner
        repository = BackendRepositoryImpl(apiService, secureStorage, poWSolver, hmacSigner)
    }

    @Test
    fun testCheckHealth_success() = runTest {
        val healthResponse = HealthResponse(
            status = "UP",
            version = "1.0.0",
            uptimeMs = 123456L,
            database = "CONNECTED",
            storage = "CONNECTED"
        )
        coEvery { apiService.getHealth() } returns Response.success(healthResponse)

        val result = repository.checkHealth()

        assertTrue(result.isSuccess)
    }

    @Test
    fun testCheckHealth_degradedStatus_returnsFailure() = runTest {
        val healthResponse = HealthResponse(
            status = "DEGRADED",
            version = "1.0.0",
            uptimeMs = 123456L,
            database = "CONNECTED",
            storage = "DISCONNECTED"
        )
        coEvery { apiService.getHealth() } returns Response.success(healthResponse)

        val result = repository.checkHealth()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkError.ServerError)
    }

    @Test
    fun testRequestChallenge_success() = runTest {
        val challenge = ChallengeResponse(seed = "testseed123", difficulty = 4)
        coEvery { apiService.getChallenge() } returns Response.success(challenge)

        val result = repository.requestChallenge()

        assertTrue(result.isSuccess)
        val (seed, difficulty) = result.getOrThrow()
        assertEquals("testseed123", seed)
        assertEquals(4, difficulty)
    }

    @Test
    fun testRequestChallenge_httpError_returnsFailure() = runTest {
        coEvery { apiService.getChallenge() } returns Response.error(
            500, "{\"error\":\"Internal Server Error\"}".toResponseBody()
        )

        val result = repository.requestChallenge()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkError)
    }

    @Test
    fun testEnrollDevice_success_returnsEnrolledDevice() = runTest {
        val seed = "aabbccdd"
        val difficulty = 1
        val challenge = ChallengeResponse(seed = seed, difficulty = difficulty)
        val enrollResponse = EnrollResponse(
            deviceId = "dev_test123",
            deviceSecret = "sec_testkey",
            createdAt = 1723000000000L
        )

        coEvery { apiService.getChallenge() } returns Response.success(challenge)
        coEvery { apiService.enrollDevice(any()) } returns Response.success(enrollResponse)

        val result = repository.enrollDevice()

        assertTrue(result.isSuccess)
        val device = result.getOrThrow()
        assertEquals("dev_test123", device.deviceId)
        assertEquals("sec_testkey", device.deviceSecret)
        assertEquals(1723000000000L, device.createdAt)

        coVerify(exactly = 1) {
            secureStorage.saveDevice(
                deviceId = "dev_test123",
                deviceSecret = "sec_testkey",
                createdAt = 1723000000000L
            )
        }
    }

    @Test
    fun testEnrollDevice_challengeFails_returnsFailure() = runTest {
        coEvery { apiService.getChallenge() } returns Response.error(
            500, "{\"error\":\"Server error\"}".toResponseBody()
        )

        val result = repository.enrollDevice()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkError)
    }

    @Test
    fun testEnrollDevice_enrollFails_returnsFailure() = runTest {
        val challenge = ChallengeResponse(seed = "seed123", difficulty = 1)
        coEvery { apiService.getChallenge() } returns Response.success(challenge)
        coEvery { apiService.enrollDevice(any()) } returns Response.error(
            400, "{\"error\":\"Invalid PoW solution\"}".toResponseBody()
        )

        val result = repository.enrollDevice()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkError)
    }

    @Test
    fun testGetEnrolledDevice_returnsStoredDevice() = runTest {
        val storedDevice = EnrolledDevice("dev_stored", "sec_stored", 12345L)
        coEvery { secureStorage.getDevice() } returns storedDevice

        val result = repository.getEnrolledDevice()

        assertNotNull(result)
        assertEquals("dev_stored", result!!.deviceId)
    }

    @Test
    fun testGetEnrolledDevice_noDeviceStored_returnsNull() = runTest {
        coEvery { secureStorage.getDevice() } returns null

        val result = repository.getEnrolledDevice()

        assertNull(result)
    }

    @Test
    fun testClearEnrollment_callsStorageClear() = runTest {
        coEvery { secureStorage.clearDevice() } returns Unit

        repository.clearEnrollment()

        coVerify(exactly = 1) { secureStorage.clearDevice() }
    }
}
