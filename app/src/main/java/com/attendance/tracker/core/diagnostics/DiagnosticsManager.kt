package com.attendance.tracker.core.diagnostics

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central entry point for the diagnostics framework.
 *
 * Responsibilities:
 *  - [install]: register the global [CrashHandler] (idempotent within a process).
 *  - [exportDiagnostics]: write the current [DiagnosticsBundle] as a single
 *    JSON file into the diagnostics directory for offline inspection.
 */
@Singleton
class DiagnosticsManager @Inject constructor(
    private val storage: DiagnosticsStorage,
    private val logBuffer: LogBuffer,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val exporter: DiagnosticsExporter,
    private val diagnosticsDirectory: File
) {

    /**
     * Installs the crash handler once, preserving any previously installed
     * handler as the delegate (when one exists).
     */
    fun install() {
        val current = Thread.getDefaultUncaughtExceptionHandler()
        if (current is CrashHandler) return
        Thread.setDefaultUncaughtExceptionHandler(
            CrashHandler(current, storage, logBuffer, deviceInfoProvider)
        )
    }

    /**
     * Exports a single JSON diagnostics file (device info + crash reports +
     * log history) into the diagnostics directory and returns it.
     */
    fun exportDiagnostics(): File {
        val file = File(
            diagnosticsDirectory,
            "${EXPORT_PREFIX}${System.currentTimeMillis()}$EXPORT_EXTENSION"
        )
        file.writeText(exporter.toJson(exporter.buildBundle()))
        return file
    }

    private companion object {
        const val EXPORT_PREFIX = "diagnostics_export_"
        const val EXPORT_EXTENSION = ".json"
    }
}
