package com.attendance.tracker.core.diagnostics

/**
 * In-memory, bounded, thread-safe holder for the most recent log entries.
 *
 * Implementations must drop the oldest entry once the capacity is exceeded so
 * memory stays bounded regardless of log volume.
 */
interface LogBuffer {

    /** Appends [entry], evicting the oldest entry when at capacity. */
    fun add(entry: LogEntry)

    /** Returns a copy of the current entries, oldest first. */
    fun snapshot(): List<LogEntry>

    /** Empties the buffer. */
    fun clear()
}
