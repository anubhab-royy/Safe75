package com.attendance.tracker.core.diagnostics

import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [LogBuffer] backed by a fixed-capacity [ArrayDeque] that keeps the most
 * recent [DEFAULT_CAPACITY] entries in insertion order.
 *
 * All access is synchronized so the logger (IO thread) and the crash handler
 * (any thread) can use the same instance safely.
 */
@Singleton
class RecentLogBuffer @Inject constructor() : LogBuffer {

    private val capacity = DEFAULT_CAPACITY
    private val entries = ArrayDeque<LogEntry>(capacity)

    @Synchronized
    override fun add(entry: LogEntry) {
        if (entries.size == capacity) {
            entries.pollFirst()
        }
        entries.addLast(entry)
    }

    @Synchronized
    override fun snapshot(): List<LogEntry> = entries.toList()

    @Synchronized
    override fun clear() = entries.clear()

    companion object {
        const val DEFAULT_CAPACITY = 100
    }
}
