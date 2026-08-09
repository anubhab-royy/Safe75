package com.attendance.tracker.feature.device.data.model

import kotlinx.serialization.Serializable

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

@Serializable
data class HealthResponse(
    val status: String,
    val version: String,
    val uptimeMs: Long,
    val database: String,
    val storage: String
)

@Serializable
data class BugReportCreateRequest(
    val timestamp: Long,
    val appVersion: String,
    val versionCode: Int,
    val buildType: String,
    val androidVersion: String,
    val sdkVersion: Int,
    val deviceManufacturer: String,
    val deviceModel: String,
    val cpuAbi: String,
    val locale: String,
    val crashReportId: String? = null,
    val userDescription: String,
    val diagnosticsMetadata: String
)

@Serializable
data class BugReportCreateResponse(
    val reportId: String,
    val status: String
)

@Serializable
data class ScreenshotUploadResponse(
    val reportId: String,
    val status: String,
    val objectKey: String
)
