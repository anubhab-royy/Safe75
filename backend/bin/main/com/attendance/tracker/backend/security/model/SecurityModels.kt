package com.attendance.tracker.backend.security.model

import kotlinx.serialization.Serializable
import org.bson.Document

enum class DeviceStatus {
    ACTIVE,
    BLOCKED
}

/**
 * Domain model representing a registered device.
 */
data class Device(
    val deviceId: String,
    val deviceSecret: String, // encrypted or secure random string
    val status: DeviceStatus,
    val createdAt: Long,
    val lastSeen: Long
)

fun Device.toDocument(): Document {
    return Document().apply {
        put("deviceId", deviceId)
        put("deviceSecret", deviceSecret)
        put("status", status.name)
        put("createdAt", createdAt)
        put("lastSeen", lastSeen)
    }
}

fun Document.toDevice(): Device {
    return Device(
        deviceId = getString("deviceId") ?: throw IllegalStateException("Missing deviceId"),
        deviceSecret = getString("deviceSecret") ?: throw IllegalStateException("Missing deviceSecret"),
        status = DeviceStatus.valueOf(getString("status") ?: "ACTIVE"),
        createdAt = getLong("createdAt") ?: 0L,
        lastSeen = getLong("lastSeen") ?: 0L
    )
}

@Serializable
data class ChallengeResponse(
    val seed: String,
    val difficulty: Int
)

@Serializable
data class EnrollRequest(
    val seed: String,
    val nonce: Long
)

@Serializable
data class EnrollResponse(
    val deviceId: String,
    val deviceSecret: String,
    val createdAt: Long
)
