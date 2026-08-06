package com.attendance.tracker.core.diagnostics

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JSON file wrapper for the rolling log history.
 */
@Serializable
internal data class LogFile(val entries: List<LogEntry> = emptyList())

/**
 * File-backed [DiagnosticsStorage].
 *
 * Layout (all under the provided diagnostics directory):
 *  - `logs.json`:        the rolling log history, capped at [MAX_LOG_ENTRIES].
 *  - `crashes/`:         one `crash_<timestamp>_<threadId>.json` per report,
 *                        capped at [MAX_CRASH_REPORTS] (oldest deleted first).
 *
 * Rotation is filename-based (timestamps sort lexicographically) so it stays
 * deterministic and needs no external clock. All operations are synchronized
 * and swallow errors so diagnostics can never break the app.
 */
@Singleton
class DiagnosticsFileStorage @Inject constructor(
    diagnosticsDirectory: File,
    private val json: Json
) : DiagnosticsStorage {

    private val crashDir = File(diagnosticsDirectory, CRASH_DIRECTORY).apply { mkdirs() }
    private val logFile = File(diagnosticsDirectory, LOG_FILE_NAME)

    @Synchronized
    override fun appendLog(entry: LogEntry) {
        runCatching {
            val entries = (readLogEntries() + entry).takeLast(MAX_LOG_ENTRIES)
            logFile.writeText(json.encodeToString(LogFile(entries)))
        }
    }

    @Synchronized
    override fun saveCrashReport(report: CrashReport) {
        runCatching {
            val file = File(crashDir, crashFileName(report.timestamp))
            file.writeText(json.encodeToString(report))
            trimCrashReports()
        }
    }

    override fun crashReports(): List<CrashReport> =
        crashFiles()
            .mapNotNull { runCatching { json.decodeFromString<CrashReport>(it.readText()) }.getOrNull() }
            .sortedByDescending { it.timestamp }

    override fun logs(): List<LogEntry> = readLogEntries()

    private fun readLogEntries(): List<LogEntry> {
        if (!logFile.exists()) return emptyList()
        return runCatching { json.decodeFromString<LogFile>(logFile.readText()).entries }
            .getOrDefault(emptyList())
    }

    private fun crashFiles(): List<File> =
        crashDir.listFiles { f ->
            f.isFile && f.name.startsWith(CRASH_PREFIX) && f.name.endsWith(JSON_EXTENSION)
        }?.sortedBy { it.name }?.toList() ?: emptyList()

    /**
     * Builds a lexicographically chronological file name by zero-padding the
     * timestamp, so name-based sorting matches actual crash order regardless
     * of the number of digits in the timestamp.
     */
    private fun crashFileName(timestamp: Long): String =
        "$CRASH_PREFIX${"%019d".format(timestamp)}-${System.nanoTime()}$JSON_EXTENSION"

    private fun trimCrashReports() {
        val files = crashFiles()
        val overflow = files.size - MAX_CRASH_REPORTS
        if (overflow > 0) {
            files.take(overflow).forEach { it.delete() }
        }
    }

    companion object {
        const val MAX_LOG_ENTRIES = 200
        const val MAX_CRASH_REPORTS = 20
        const val CRASH_DIRECTORY = "crashes"
        private const val LOG_FILE_NAME = "logs.json"
        private const val CRASH_PREFIX = "crash_"
        private const val JSON_EXTENSION = ".json"
    }
}
