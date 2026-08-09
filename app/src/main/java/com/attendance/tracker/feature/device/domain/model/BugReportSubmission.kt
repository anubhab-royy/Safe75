package com.attendance.tracker.feature.device.domain.model

data class BugReportSubmission(
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
    val diagnosticsMetadata: String
)
