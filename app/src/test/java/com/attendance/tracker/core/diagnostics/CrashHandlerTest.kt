package com.attendance.tracker.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Unit tests for [CrashHandler] capture and delegation behavior.
 */
class CrashHandlerTest {

    private class FakeStorage : DiagnosticsStorage {
        val reports = mutableListOf<CrashReport>()
        override fun appendLog(entry: LogEntry) = Unit
        override fun saveCrashReport(report: CrashReport) { reports += report }
        override fun crashReports(): List<CrashReport> = reports
        override fun logs(): List<LogEntry> = emptyList()
    }

    private class RecordingPrevious : Thread.UncaughtExceptionHandler {
        var thread: Thread? = null
        var throwable: Throwable? = null
        override fun uncaughtException(t: Thread, e: Throwable) {
            thread = t
            throwable = e
        }
    }

    @Test
    fun testUncaughtException_savesReportAndDelegates() {
        val storage = FakeStorage()
        val buffer = RecentLogBuffer()
        buffer.add(LogEntry(1L, LogLevel.ERROR, "tag", "before crash"))
        val previous = RecordingPrevious()
        val handler = CrashHandler(
            previous = previous,
            storage = storage,
            logBuffer = buffer,
            deviceInfoProvider = FakeDeviceProvider()
        )

        val throwable = NullPointerException("boom")
        handler.uncaughtException(Thread.currentThread(), throwable)

        assertEquals(1, storage.reports.size)
        val report = storage.reports.first()
        assertEquals("java.lang.NullPointerException", report.exceptionClass)
        assertEquals("boom", report.message)
        assertEquals(Thread.currentThread().name, report.threadName)
        assertEquals("1.0.0", report.appVersion)
        assertEquals("release", report.buildType)
        assertEquals(1, report.recentLogs.size)
        assertEquals("before crash", report.recentLogs.first().message)
        assertSame(throwable, previous.throwable)
        assertSame(Thread.currentThread(), previous.thread)
    }

    @Test
    fun testUncaughtException_recentLogsSnapshotCaptured() {
        val storage = FakeStorage()
        val buffer = RecentLogBuffer()
        repeat(5) { buffer.add(LogEntry(it.toLong(), LogLevel.DEBUG, "t", "log-$it")) }
        val handler = CrashHandler(storage = storage, logBuffer = buffer)

        handler.uncaughtException(Thread.currentThread(), RuntimeException("x"))

        assertEquals(listOf("log-0", "log-1", "log-2", "log-3", "log-4"),
            storage.reports.first().recentLogs.map { it.message })
    }

    private fun CrashHandler(
        storage: DiagnosticsStorage,
        logBuffer: LogBuffer
    ) = CrashHandler(
        previous = Thread.UncaughtExceptionHandler { _, _ -> },
        storage = storage,
        logBuffer = logBuffer,
        deviceInfoProvider = FakeDeviceProvider()
    )

    private class FakeDeviceProvider : DeviceInfoProvider {
        override fun deviceInfo() = DeviceInfo(
            appVersion = "1.0.0",
            versionCode = 1,
            buildType = "release",
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
}
