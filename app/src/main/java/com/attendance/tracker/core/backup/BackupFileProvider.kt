package com.attendance.tracker.core.backup

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supplies file-name conventions and extension validation for exported backups.
 *
 * Kept pure (no Android dependencies) so it can be unit-tested in isolation
 * and shared by both the UI and the backup manager.
 */
@Singleton
class BackupFileProvider @Inject constructor() {

    private val stampFormat: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    /** Default backup file prefix, e.g. "safe75_backup_". */
    val defaultPrefix: String = DEFAULT_BACKUP_PREFIX

    /** Default backup file extension, e.g. ".json". */
    val defaultExtension: String = DEFAULT_BACKUP_EXTENSION

    /**
     * Builds a timestamped backup file name, e.g.
     * `safe75_backup_20260802_143000.json`.
     *
     * @param now Epoch millis; defaults to the current system time.
     */
    fun createDefaultFileName(now: Long = System.currentTimeMillis()): String =
        createFileName(prefix = DEFAULT_BACKUP_PREFIX, now = now)

    /**
     * Builds a timestamped backup file name with a custom [prefix].
     *
     * @param prefix Base file prefix (without extension).
     * @param now    Epoch millis; defaults to the current system time.
     */
    fun createFileName(prefix: String, now: Long = System.currentTimeMillis()): String {
        val stamp = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(now),
            java.time.ZoneId.systemDefault()
        ).format(stampFormat)
        return "$prefix$stamp$DEFAULT_BACKUP_EXTENSION"
    }

    /**
     * Returns true when [name] ends with `.json` (case-insensitive).
     */
    fun hasJsonExtension(name: String): Boolean {
        if (name.isBlank()) return false
        val lower = name.lowercase()
        return lower.endsWith(DEFAULT_BACKUP_EXTENSION)
    }

    /**
     * Returns the suggested display name shown in the system file picker.
     */
    fun suggestedExportFileName(now: Long = System.currentTimeMillis()): String =
        createDefaultFileName(now)

    companion object {
        private const val DEFAULT_BACKUP_PREFIX = "safe75_backup_"
        private const val DEFAULT_BACKUP_EXTENSION = ".json"
    }
}
