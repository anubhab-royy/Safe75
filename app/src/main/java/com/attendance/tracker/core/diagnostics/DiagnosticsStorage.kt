package com.attendance.tracker.core.diagnostics

/**
 * Persistent, bounded storage for diagnostics data.
 *
 * The contract is platform-free so implementations (file-based today) can be
 * swapped without affecting callers. Implementations must be safe to call from
 * any thread and must never throw; failures degrade gracefully.
 */
interface DiagnosticsStorage {

    /** Appends [entry] to the persistent log, trimming to the retention cap. */
    fun appendLog(entry: LogEntry)

    /** Persists [report], trimming to the retention cap. */
    fun saveCrashReport(report: CrashReport)

    /** Returns persisted crash reports, newest first. */
    fun crashReports(): List<CrashReport>

    /** Returns persisted log entries, oldest first. */
    fun logs(): List<LogEntry>
}
