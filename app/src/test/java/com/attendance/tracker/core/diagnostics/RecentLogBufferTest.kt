package com.attendance.tracker.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [RecentLogBuffer] capacity and eviction behavior.
 */
class RecentLogBufferTest {

    private val buffer = RecentLogBuffer()

    private fun entry(timestamp: Long) = LogEntry(
        timestamp = timestamp,
        level = LogLevel.INFO,
        tag = "tag",
        message = "message-$timestamp"
    )

    @Test
    fun testAdd_withinCapacity_keepsAllInOrder() {
        repeat(50) { buffer.add(entry(it.toLong())) }

        val snapshot = buffer.snapshot()
        assertEquals(50, snapshot.size)
        assertEquals("message-0", snapshot.first().message)
        assertEquals("message-49", snapshot.last().message)
    }

    @Test
    fun testAdd_overCapacity_evictsOldest() {
        repeat(120) { buffer.add(entry(it.toLong())) }

        val snapshot = buffer.snapshot()
        assertEquals(RecentLogBuffer.DEFAULT_CAPACITY, snapshot.size)
        assertEquals("message-20", snapshot.first().message)
        assertEquals("message-119", snapshot.last().message)
    }

    @Test
    fun testClear_emptiesBuffer() {
        buffer.add(entry(1L))
        buffer.clear()
        assertEquals(0, buffer.snapshot().size)
    }

    @Test
    fun testSnapshot_returnsCopyNotLiveView() {
        buffer.add(entry(1L))
        val snapshot = buffer.snapshot()
        buffer.clear()
        assertEquals(0, buffer.snapshot().size)
        assertEquals(1, snapshot.size)
    }
}
