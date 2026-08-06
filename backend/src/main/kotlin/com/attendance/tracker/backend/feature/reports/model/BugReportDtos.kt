package com.attendance.tracker.backend.feature.reports.model

import kotlinx.serialization.Serializable

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
data class BugReportResponse(
    val reportId: String,
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
    val diagnosticsMetadata: String,
    val uploadStatus: String,
    val retryCount: Int,
    val screenshotObjectKey: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Extension to convert a [BugReport] domain model to its corresponding [BugReportResponse] DTO.
 */
fun BugReport.toDto(): BugReportResponse {
    return BugReportResponse(
        reportId = reportId,
        timestamp = timestamp,
        appVersion = appVersion,
        versionCode = versionCode,
        buildType = buildType,
        androidVersion = androidVersion,
        sdkVersion = sdkVersion,
        deviceManufacturer = deviceManufacturer,
        deviceModel = deviceModel,
        cpuAbi = cpuAbi,
        locale = locale,
        crashReportId = crashReportId,
        userDescription = userDescription,
        diagnosticsMetadata = diagnosticsMetadata,
        uploadStatus = uploadStatus.name,
        retryCount = retryCount,
        screenshotObjectKey = screenshotObjectKey,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
