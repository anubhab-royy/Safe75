package com.attendance.tracker.core.diagnostics

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [SafeLogEngine] buffer + storage recording (logcat disabled).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SafeLogEngineTest {

    private class FakeStorage : DiagnosticsStorage {
        val entries = mutableListOf<LogEntry>()
        override fun appendLog(entry: LogEntry) { entries += entry }
        override fun saveCrashReport(report: CrashReport) = Unit
        override fun crashReports(): List<CrashReport> = emptyList()
        override fun logs(): List<LogEntry> = entries
    }

    @Test
    fun testLog_recordsToBufferAndStorage() = runTest {
        val storage = FakeStorage()
        val buffer = RecentLogBuffer()
        val engine = SafeLogEngine(storage, buffer, this, logcatEnabled = false)

        engine.e("tag", "failure occurred", IllegalStateException("cause"))
        advanceUntilIdle()

        val buffered = buffer.snapshot().first()
        assertEquals(LogLevel.ERROR, buffered.level)
        assertEquals("tag", buffered.tag)
        assertEquals("failure occurred", buffered.message)
        assertEquals(1, storage.entries.size)
        assertEquals(buffered, storage.entries.first())
    }

    @Test
    fun testLog_mapsLevels() = runTest {
        val storage = FakeStorage()
        val engine = SafeLogEngine(storage, RecentLogBuffer(), this, logcatEnabled = false)

        engine.d("t", "debug")
        engine.i("t", "info")
        engine.w("t", "warn")
        engine.e("t", "error")
        advanceUntilIdle()

        val levels = bufferLevels(storage.entries)
        assertEquals(
            listOf(LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARNING, LogLevel.ERROR),
            levels
        )
    }

    @Test
    fun testLog_throwableFormattedButOptional() = runTest {
        val storage = FakeStorage()
        val engine = SafeLogEngine(storage, RecentLogBuffer(), this, logcatEnabled = false)

        engine.w("t", "no throwable")
        engine.e("t", "with throwable", NullPointerException("npe"))
        advanceUntilIdle()

        assertEquals(null, storage.entries[0].throwable)
        assertEquals(true, storage.entries[1].throwable?.contains("NullPointerException") == true)
    }

    private fun bufferLevels(entries: List<LogEntry>): List<LogLevel> =
        entries.map { it.level }
}
