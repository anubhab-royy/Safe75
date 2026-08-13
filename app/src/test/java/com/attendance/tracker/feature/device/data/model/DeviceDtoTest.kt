package com.attendance.tracker.feature.device.data.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DeviceDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testChallengeResponse_serialization() {
        val dto = ChallengeResponse(seed = "aabbccdd12345678", difficulty = 4)
        val encoded = json.encodeToString(ChallengeResponse.serializer(), dto)
        val decoded = json.decodeFromString(ChallengeResponse.serializer(), encoded)

        assertEquals(dto.seed, decoded.seed)
        assertEquals(dto.difficulty, decoded.difficulty)
    }

    @Test
    fun testChallengeResponse_deserialization_fromBackendJson() {
        val backendJson = """{"seed":"aabbccdd12345678","difficulty":4}"""
        val dto = json.decodeFromString(ChallengeResponse.serializer(), backendJson)

        assertEquals("aabbccdd12345678", dto.seed)
        assertEquals(4, dto.difficulty)
    }

    @Test
    fun testEnrollRequest_serialization() {
        val request = EnrollRequest(seed = "aabbccdd12345678ef", nonce = 12345L)
        val encoded = json.encodeToString(EnrollRequest.serializer(), request)
        val decoded = json.decodeFromString(EnrollRequest.serializer(), encoded)

        assertEquals(request.seed, decoded.seed)
        assertEquals(request.nonce, decoded.nonce)
    }

    @Test
    fun testEnrollResponse_deserialization_fromBackendJson() {
        val backendJson = """{"deviceId":"dev_abc-123","deviceSecret":"sec_mysecretkey","createdAt":1723000000000}"""
        val dto = json.decodeFromString(EnrollResponse.serializer(), backendJson)

        assertEquals("dev_abc-123", dto.deviceId)
        assertEquals("sec_mysecretkey", dto.deviceSecret)
        assertEquals(1723000000000L, dto.createdAt)
    }

    @Test
    fun testEnrollResponse_serialization_roundTrip() {
        val response = EnrollResponse(
            deviceId = "dev_test-456",
            deviceSecret = "sec_abcdef0123456789",
            createdAt = 9999999999999L
        )
        val encoded = json.encodeToString(EnrollResponse.serializer(), response)
        val decoded = json.decodeFromString(EnrollResponse.serializer(), encoded)

        assertEquals(response, decoded)
    }

    @Test
    fun testHealthResponse_deserialization_fromBackendJson() {
        val backendJson = """{"status":"UP","version":"1.0.0","uptimeMs":12345678,"database":"CONNECTED","storage":"CONNECTED"}"""
        val dto = json.decodeFromString(HealthResponse.serializer(), backendJson)

        assertEquals("UP", dto.status)
        assertEquals("1.0.0", dto.version)
        assertEquals(12345678L, dto.uptimeMs)
        assertEquals("CONNECTED", dto.database)
        assertEquals("CONNECTED", dto.storage)
    }

    @Test
    fun testEnrollResponse_ignoresUnknownKeys() {
        val backendJson = """{"deviceId":"dev_123","deviceSecret":"sec_456","createdAt":1000,"extraField":"ignored"}"""
        val dto = json.decodeFromString(EnrollResponse.serializer(), backendJson)

        assertNotNull(dto)
        assertEquals("dev_123", dto.deviceId)
    }
}
