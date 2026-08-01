package com.attendance.tracker.core.backup

import kotlinx.serialization.Serializable

/** Current schema version. Bump this when the backup format changes incompatibly. */
const val CURRENT_SCHEMA_VERSION = 1

/** Current backup format version. */
const val CURRENT_BACKUP_VERSION = 1

/**
 * Metadata block included at the top of every backup file.
 *
 * @property backupVersion   Monotonically increasing backup format revision.
 * @property appVersion      App versionName at the time of export.
 * @property schemaVersion   Schema revision used to validate import compatibility.
 * @property createdAt       Epoch millis when the backup was created.
 */
@Serializable
data class BackupMetadata(
    val backupVersion: Int = CURRENT_BACKUP_VERSION,
    val appVersion: String = "1.0",
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val createdAt: Long = System.currentTimeMillis()
)
