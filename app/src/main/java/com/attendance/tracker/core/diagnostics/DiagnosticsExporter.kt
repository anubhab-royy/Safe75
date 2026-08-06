package com.attendance.tracker.core.diagnostics

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds and serializes the single-file diagnostics export bundle.
 *
 * The bundle merges persisted logs with any log entries still sitting in the
 * in-memory [LogBuffer] (deduplicating identical entries) and is purely
 * offline — this component never transmits data.
 */
@Singleton
class DiagnosticsExporter @Inject constructor(
    private val storage: DiagnosticsStorage,
    private val logBuffer: LogBuffer,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val json: Json
) {

    /**
     * Assembles the current export [DiagnosticsBundle].
     */
    fun buildBundle(exportedAt: Long = System.currentTimeMillis()): DiagnosticsBundle {
        val mergedLogs = (storage.logs() + logBuffer.snapshot()).distinct()
        return DiagnosticsBundle(
            formatVersion = FORMAT_VERSION,
            exportedAt = exportedAt,
            deviceInfo = deviceInfoProvider.deviceInfo(),
            crashReports = storage.crashReports(),
            logs = mergedLogs
        )
    }

    /**
     * Serializes [bundle] to a pretty-printed JSON string.
     */
    fun toJson(bundle: DiagnosticsBundle): String = json.encodeToString(bundle)

    companion object {
        const val FORMAT_VERSION = 1
    }
}
