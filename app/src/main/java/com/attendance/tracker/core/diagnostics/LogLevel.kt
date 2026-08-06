package com.attendance.tracker.core.diagnostics

import kotlinx.serialization.Serializable

/**
 * Severity levels used by the diagnostics logger. Each entry is stored with
 * its level so crash reports and exports can be filtered or ranked later.
 */
@Serializable
enum class LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR
}
