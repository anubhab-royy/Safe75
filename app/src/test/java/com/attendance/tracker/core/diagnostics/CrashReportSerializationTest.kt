package com.attendance.tracker.core.diagnostics

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Round-trip serialization tests for all diagnostics data classes.
 */
class CrashReportSerializationTest {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun testCrashReport_roundTrip() {
        val report = CrashReport(
            id = "1_42",
            timestamp = 1_700_000_000_000L,
            threadName = "main",
            exceptionClass = "java.lang.NullPointerException",
            message = "null!",
            stackTrace = listOf("\tat com.example.B.c(B.kt:3)", "\tat com.example.B.d(B.kt:9)"),
            causeClass = "java.lang.IllegalStateException",
            causeMessage = "caused",
            appVersion = "1.0.0",
            versionCode = 1,
            buildType = "release",
            deviceInfo = deviceInfo(),
            recentLogs = listOf(LogEntry(1_700_000_000_000L, LogLevel.ERROR, "tag", "last log"))
        )

        val decoded = json.decodeFromString<CrashReport>(json.encodeToString(report))

        assertEquals(report, decoded)
    }

    @Test
    fun testLogEntry_roundTrip() {
        val entry = LogEntry(123L, LogLevel.WARNING, "tag", "message", "stack")

        val decoded = json.decodeFromString<LogEntry>(json.encodeToString(entry))

        assertEquals(entry, decoded)
    }

    @Test
    fun testDeviceInfo_roundTrip() {
        val decoded = json.decodeFromString<DeviceInfo>(json.encodeToString(deviceInfo()))

        assertEquals(deviceInfo(), decoded)
    }

    @Test
    fun testDiagnosticsBundle_roundTrip() {
        val bundle = DiagnosticsBundle(
            formatVersion = 1,
            exportedAt = 123L,
            deviceInfo = deviceInfo(),
            crashReports = emptyList(),
            logs = listOf(LogEntry(1L, LogLevel.DEBUG, "t", "m"))
        )

        val decoded = json.decodeFromString<DiagnosticsBundle>(json.encodeToString(bundle))

        assertEquals(bundle, decoded)
    }

    private fun deviceInfo() = DeviceInfo(
        appVersion = "1.0.0",
        versionCode = 1,
        buildType = "release",
        androidVersion = "15",
        sdkInt = 35,
        manufacturer = "Google",
        model = "Pixel 9",
        brand = "google",
        abi = "arm64-v8a",
        locale = "en-US",
        densityDpi = 420,
        memoryClassMb = 256
    )
}
