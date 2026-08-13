package com.attendance.tracker.feature.device.data.remote

import com.attendance.tracker.feature.device.data.security.SecureDeviceStorage
import com.attendance.tracker.feature.device.domain.model.EnrolledDevice
import io.mockk.every
import io.mockk.mockk
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

class SigningInterceptorTest {

    private lateinit var mockWebServer: MockWebServer

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testInterceptor_addsSecurityHeaders_toAuthenticatedRequest() {
        val deviceId = "dev_test123"
        val deviceSecret = "sec_testkey"
        val mockStorage = mockk<SecureDeviceStorage>()
        every { mockStorage.getDevice() } returns EnrolledDevice(deviceId, deviceSecret, 0L)
        val interceptor = SigningInterceptor(mockStorage)

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val body = """{"key":"value"}""".toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .post(body)
            .build()

        client.newCall(request).execute()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals(deviceId, recordedRequest.getHeader("X-Safe75-DeviceId"))
        assertTrue(!recordedRequest.getHeader("X-Safe75-Signature").isNullOrBlank())
        assertTrue(!recordedRequest.getHeader("X-Safe75-Nonce").isNullOrBlank())
        assertTrue(recordedRequest.getHeader("X-Safe75-Timestamp")?.toLongOrNull() != null)
    }

    @Test
    fun testInterceptor_noHeaders_whenDeviceNotEnrolled() {
        val mockStorage = mockk<SecureDeviceStorage>()
        every { mockStorage.getDevice() } returns null
        val interceptor = SigningInterceptor(mockStorage)

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder()
            .url(mockWebServer.url("/test"))
            .get()
            .build()

        client.newCall(request).execute()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals(null, recordedRequest.getHeader("X-Safe75-DeviceId"))
        assertEquals(null, recordedRequest.getHeader("X-Safe75-Signature"))
        assertEquals(null, recordedRequest.getHeader("X-Safe75-Nonce"))
        assertEquals(null, recordedRequest.getHeader("X-Safe75-Timestamp"))
    }

    @Test
    fun testInterceptor_generatesUniqueNonces() {
        val mockStorage = mockk<SecureDeviceStorage>()
        every { mockStorage.getDevice() } returns EnrolledDevice("dev_123", "sec_456", 0L)
        val interceptor = SigningInterceptor(mockStorage)

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        repeat(3) {
            mockWebServer.enqueue(MockResponse().setResponseCode(200))
            val request = Request.Builder()
                .url(mockWebServer.url("/test"))
                .get()
                .build()
            client.newCall(request).execute()
        }

        val nonces = mutableSetOf<String>()
        repeat(3) {
            val recorded = mockWebServer.takeRequest()
            nonces.add(recorded.getHeader("X-Safe75-Nonce") ?: "")
        }

        assertEquals(3, nonces.size)
    }
}
