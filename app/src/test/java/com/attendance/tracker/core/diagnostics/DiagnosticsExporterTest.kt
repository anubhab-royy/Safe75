package com.attendance.tracker.core.diagnostics

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [DiagnosticsExporter] bundle assembly and JSON export.
 */
class DiagnosticsExporterTest {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private class FakeStorage : DiagnosticsStorage {
        val savedLogs = mutableListOf<LogEntry>()
        val savedReports = mutableListOf<CrashReport>()
        override fun appendLog(entry: LogEntry) { savedLogs += entry }
        override fun saveCrashReport(report: CrashReport) { savedReports += report }
        override fun crashReports(): List<CrashReport> = savedReports
        override fun logs(): List<LogEntry> = savedLogs
    }

    private class FakeDeviceProvider(private val info: DeviceInfo) : DeviceInfoProvider {
        override fun deviceInfo(): DeviceInfo = info
    }

    @Test
    fun testBuildBundle_containsDeviceCrashAndLogs() {
        val storage = FakeStorage()
        storage.appendLog(logEntry(1L))
        storage.saveCrashReport(report(5L))
        val exporter = DiagnosticsExporter(storage, RecentLogBuffer(), FakeDeviceProvider(deviceInfo()), json)

        val bundle = exporter.buildBundle(exportedAt = 99L)

        assertEquals(DiagnosticsExporter.FORMAT_VERSION, bundle.formatVersion)
        assertEquals(99L, bundle.exportedAt)
        assertEquals("1.0.0", bundle.deviceInfo.appVersion)
        assertEquals(1, bundle.crashReports.size)
        assertEquals(1, bundle.logs.size)
        assertTrue(bundle.crashReports.first().stackTrace.isNotEmpty())
    }

    @Test
    fun testBuildBundle_mergesBufferEntriesNotYetPersisted() {
        val storage = FakeStorage()
        val buffer = RecentLogBuffer()
        buffer.add(logEntry(2L))
        val exporter = DiagnosticsExporter(storage, buffer, FakeDeviceProvider(deviceInfo()), json)

        val bundle = exporter.buildBundle()

        assertEquals(listOf(2L), bundle.logs.map { it.timestamp })
    }

    @Test
    fun testBuildBundle_deduplicatesEntriesPresentInBoth() {
        val storage = FakeStorage()
        storage.appendLog(logEntry(1L))
        val buffer = RecentLogBuffer()
        buffer.add(logEntry(1L))
        val exporter = DiagnosticsExporter(storage, buffer, FakeDeviceProvider(deviceInfo()), json)

        val bundle = exporter.buildBundle()

        assertEquals(listOf(1L), bundle.logs.map { it.timestamp })
    }

    @Test
    fun testToJson_roundTrips() {
        val storage = FakeStorage()
        storage.saveCrashReport(report(5L))
        val exporter = DiagnosticsExporter(storage, RecentLogBuffer(), FakeDeviceProvider(deviceInfo()), json)

        val bundle = exporter.buildBundle()
        val decoded = json.decodeFromString<DiagnosticsBundle>(exporter.toJson(bundle))

        assertEquals(bundle, decoded)
    }

    private fun logEntry(timestamp: Long) =
        LogEntry(timestamp, LogLevel.WARNING, "tag", "warn")

    private fun report(timestamp: Long) = CrashReport(
        id = "id-$timestamp",
        timestamp = timestamp,
        threadName = "main",
        exceptionClass = "java.lang.IllegalStateException",
        message = "state",
        stackTrace = listOf("\tat com.example.A.b(A.kt:2)"),
        appVersion = "1.0.0",
        versionCode = 1,
        buildType = "debug",
        deviceInfo = deviceInfo(),
        recentLogs = emptyList()
    )

    private fun deviceInfo() = DeviceInfo(
        appVersion = "1.0.0",
        versionCode = 1,
        buildType = "debug",
        androidVersion = "15",
        sdkInt = 35,
        manufacturer = "Google",
        model = "Pixel",
        brand = "google",
        abi = "arm64-v8a",
        locale = "en-US",
        densityDpi = 420,
        memoryClassMb = 256
    )
}
