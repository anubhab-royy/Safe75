package com.attendance.tracker.backend.feature.reports.model

import org.bson.Document

enum class UploadStatus {
    PENDING,
    UPLOADED,
    FAILED,
    SCREENSHOT_PENDING,
    SCREENSHOT_UPLOADED,
    VALIDATION_FAILED,
    UPLOAD_FAILED
}

/**
 * Domain representation of a bug report.
 */
data class BugReport(
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
    val crashReportId: String?,
    val userDescription: String,
    val diagnosticsMetadata: String, // diagnostics JSON
    val uploadStatus: UploadStatus,
    val retryCount: Int,
    val screenshotObjectKey: String?,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Converts a [BugReport] domain model into a MongoDB [Document].
 */
fun BugReport.toDocument(): Document {
    return Document().apply {
        put("reportId", reportId)
        put("timestamp", timestamp)
        put("appVersion", appVersion)
        put("versionCode", versionCode)
        put("buildType", buildType)
        put("androidVersion", androidVersion)
        put("sdkVersion", sdkVersion)
        put("deviceManufacturer", deviceManufacturer)
        put("deviceModel", deviceModel)
        put("cpuAbi", cpuAbi)
        put("locale", locale)
        put("crashReportId", crashReportId)
        put("userDescription", userDescription)
        put("diagnosticsMetadata", diagnosticsMetadata)
        put("uploadStatus", uploadStatus.name)
        put("retryCount", retryCount)
        put("screenshotObjectKey", screenshotObjectKey)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }
}

/**
 * Reconstructs a [BugReport] domain model from a MongoDB [Document].
 */
fun Document.toBugReport(): BugReport {
    return BugReport(
        reportId = getString("reportId") ?: throw IllegalStateException("Missing reportId"),
        timestamp = getLong("timestamp") ?: 0L,
        appVersion = getString("appVersion") ?: "",
        versionCode = getInteger("versionCode") ?: 0,
        buildType = getString("buildType") ?: "",
        androidVersion = getString("androidVersion") ?: "",
        sdkVersion = getInteger("sdkVersion") ?: 0,
        deviceManufacturer = getString("deviceManufacturer") ?: "",
        deviceModel = getString("deviceModel") ?: "",
        cpuAbi = getString("cpuAbi") ?: "",
        locale = getString("locale") ?: "",
        crashReportId = getString("crashReportId"),
        userDescription = getString("userDescription") ?: "",
        diagnosticsMetadata = getString("diagnosticsMetadata") ?: "",
        uploadStatus = UploadStatus.valueOf(getString("uploadStatus") ?: "PENDING"),
        retryCount = getInteger("retryCount") ?: 0,
        screenshotObjectKey = getString("screenshotObjectKey"),
        createdAt = getLong("createdAt") ?: 0L,
        updatedAt = getLong("updatedAt") ?: 0L
    )
}
