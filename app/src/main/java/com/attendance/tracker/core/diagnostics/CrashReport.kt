package com.attendance.tracker.core.diagnostics

import kotlinx.serialization.Serializable

/**
 * Persistent record of an uncaught exception.
 *
 * Captures the exception class, message, formatted stack trace, the crashing
 * thread, app/device environment, and the most recent log entries leading up
 * to the crash.
 */
@Serializable
data class CrashReport(
    val id: String,
    val timestamp: Long,
    val threadName: String,
    val exceptionClass: String,
    val message: String,
    val stackTrace: List<String>,
    val causeClass: String? = null,
    val causeMessage: String? = null,
    val appVersion: String,
    val versionCode: Int,
    val buildType: String,
    val deviceInfo: DeviceInfo,
    val recentLogs: List<LogEntry>
)
