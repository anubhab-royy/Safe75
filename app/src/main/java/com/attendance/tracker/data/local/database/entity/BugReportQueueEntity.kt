package com.attendance.tracker.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Durable local state for one manually submitted bug report. */
@Entity(
    tableName = "bug_report_queue",
    indices = [Index(value = ["fingerprint"], unique = true)]
)
data class BugReportQueueEntity(
    @PrimaryKey val reportId: String,
    val fingerprint: String,
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
    val diagnosticsMetadata: String,
    val screenshotPath: String?,
    val screenshotContentType: String?,
    val remoteReportId: String?,
    val status: String,
    val retryCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val lastError: String?
)
