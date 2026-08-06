package com.attendance.tracker.core.diagnostics

import kotlinx.serialization.Serializable

/**
 * The single-file export payload produced by [DiagnosticsExporter].
 *
 * Combines the current device environment, retained crash reports, and the
 * retained log history into one JSON document. Purely offline: the bundle is
 * never transmitted anywhere by this framework.
 */
@Serializable
data class DiagnosticsBundle(
    val formatVersion: Int,
    val exportedAt: Long,
    val deviceInfo: DeviceInfo,
    val crashReports: List<CrashReport>,
    val logs: List<LogEntry>
)
