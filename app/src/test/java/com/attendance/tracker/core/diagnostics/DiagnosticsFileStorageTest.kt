package com.attendance.tracker.core.diagnostics

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit tests for [DiagnosticsFileStorage] persistence and rotation caps.
 */
class DiagnosticsFileStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun storage(): DiagnosticsFileStorage =
        DiagnosticsFileStorage(tempFolder.root, json)

    private fun logEntry(timestamp: Long) =
        LogEntry(timestamp, LogLevel.INFO, "tag", "message-$timestamp")

    private fun crashReport(timestamp: Long) = CrashReport(
        id = "$timestamp-42",
        timestamp = timestamp,
        threadName = "main",
        exceptionClass = "java.lang.RuntimeException",
        message = "boom-$timestamp",
        stackTrace = listOf("\tat com.example.Foo.bar(Foo.kt:1)"),
        appVersion = "1.0.0",
        versionCode = 1,
        buildType = "debug",
        deviceInfo = DeviceInfoBuilder().unknown(),
        recentLogs = emptyList()
    )

    @Test
    fun testAppendLog_persistsEntries() {
        val store = storage()
        store.appendLog(logEntry(1L))
        store.appendLog(logEntry(2L))

        assertEquals(listOf(1L, 2L), store.logs().map { it.timestamp })
    }

    @Test
    fun testAppendLog_trimsToMaxEntries() {
        val store = storage()
        repeat(DiagnosticsFileStorage.MAX_LOG_ENTRIES + 50) { store.appendLog(logEntry(it.toLong())) }

        val logs = store.logs()
        assertEquals(DiagnosticsFileStorage.MAX_LOG_ENTRIES, logs.size)
        assertEquals(50L, logs.first().timestamp)
        assertEquals(DiagnosticsFileStorage.MAX_LOG_ENTRIES + 49L, logs.last().timestamp)
    }

    @Test
    fun testSaveCrashReport_roundTrips() {
        val store = storage()
        store.saveCrashReport(crashReport(10L))

        val loaded = store.crashReports()
        assertEquals(1, loaded.size)
        assertEquals("boom-10", loaded.first().message)
        assertEquals("java.lang.RuntimeException", loaded.first().exceptionClass)
    }

    @Test
    fun testSaveCrashReport_rotatesToMaxReportsNewestFirst() {
        val store = storage()
        repeat(DiagnosticsFileStorage.MAX_CRASH_REPORTS + 5) { store.saveCrashReport(crashReport(it.toLong())) }

        val loaded = store.crashReports()
        assertEquals(DiagnosticsFileStorage.MAX_CRASH_REPORTS, loaded.size)
        assertEquals(DiagnosticsFileStorage.MAX_CRASH_REPORTS + 4L, loaded.first().timestamp)
        assertEquals(5L, loaded.last().timestamp)
    }

    @Test
    fun testCrashReports_ignoresCorruptFiles() {
        val store = storage()
        store.saveCrashReport(crashReport(10L))
        File(File(tempFolder.root, DiagnosticsFileStorage.CRASH_DIRECTORY), "crash_999_corrupt.json")
            .writeText("{ not json")

        assertEquals(1, store.crashReports().size)
    }

    @Test
    fun testLogs_handlesMissingFile() {
        assertEquals(0, storage().logs().size)
    }
}
