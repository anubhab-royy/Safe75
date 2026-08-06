package com.attendance.tracker.core.diagnostics

import kotlinx.serialization.Serializable

/**
 * A single structured log line captured by the diagnostics logger.
 *
 * Only technical data is recorded: timestamp, level, tag, message, and an
 * optional pre-formatted throwable. No user or business data is logged.
 */
@Serializable
data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: String? = null
)
