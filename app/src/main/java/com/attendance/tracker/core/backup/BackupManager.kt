package com.attendance.tracker.core.backup

import kotlinx.serialization.SerializationException
import java.io.FileNotFoundException
import java.io.IOException
import java.security.AccessControlException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Outcome of parsing + validating a raw backup JSON string.
 */
sealed class BackupParseResult {
    /** The JSON was parsed and passed every validation rule. */
    data class Parsed(val data: BackupData) : BackupParseResult()

    /** The JSON was rejected with a typed [BackupError]. */
    data class Rejected(val error: BackupError) : BackupParseResult()
}

/**
 * Central orchestrator for the backup JSON lifecycle.
 *
 * Owns the single serialization/validation pipeline so no other component
 * duplicates parse + validate logic (see Quality: no duplicated serialization logic).
 *
 * @property serializer    Encodes [BackupData] to JSON.
 * @property deserializer  Decodes JSON to [BackupData].
 * @property validator     Applies structural and referential checks.
 * @property fileProvider  Supplies file-name conventions.
 */
@Singleton
class BackupManager @Inject constructor(
    private val serializer: BackupSerializer,
    private val deserializer: BackupDeserializer,
    private val validator: BackupValidator,
    private val fileProvider: BackupFileProvider
) {

    /**
     * Serializes a [BackupData] snapshot to a formatted JSON string.
     */
    fun serialize(data: BackupData): String = serializer.serialize(data)

    /**
     * Parses and validates a raw backup JSON string.
     *
     * Returns [BackupParseResult.Parsed] only when the JSON is structurally valid,
     * the backup/schema versions are supported, and all referential checks pass.
     * Otherwise returns [BackupParseResult.Rejected] with a typed [BackupError].
     *
     * @param json Raw file content.
     */
    fun parseBackup(json: String): BackupParseResult {
        if (json.isBlank()) {
            return BackupParseResult.Rejected(BackupError.InvalidJson("Backup file is empty."))
        }

        val data = deserializer.deserialize(json).getOrElse { error ->
            return BackupParseResult.Rejected(
                BackupError.InvalidJson("Invalid backup format: ${error.message}")
            )
        }

        if (data.metadata.schemaVersion > CURRENT_SCHEMA_VERSION) {
            return BackupParseResult.Rejected(
                BackupError.UnsupportedSchema(
                    "Unsupported schema version ${data.metadata.schemaVersion}. " +
                    "Maximum supported version is $CURRENT_SCHEMA_VERSION. Please update the app."
                )
            )
        }

        if (data.metadata.backupVersion > CURRENT_BACKUP_VERSION) {
            return BackupParseResult.Rejected(
                BackupError.OldBackupVersion(
                    "This backup was created by a newer app version " +
                    "(backup format ${data.metadata.backupVersion}). Please update the app to restore it."
                )
            )
        }

        return when (val result = validator.validate(data)) {
            is ValidationResult.Valid -> BackupParseResult.Parsed(result.data)
            is ValidationResult.Invalid -> BackupParseResult.Rejected(
                BackupError.InvalidJson(
                    "Validation failed:\n" + result.errors.joinToString("\n")
                )
            )
        }
    }

    /**
     * Validates an already-parsed [BackupData] and returns a [ValidationResult].
     * Primarily used when a preview is required before import.
     */
    fun validate(data: BackupData): ValidationResult = validator.validate(data)

    /**
     * Maps an arbitrary [Throwable] to a typed [BackupError] for consistent error
     * handling at the UI boundary.
     */
    fun classifyError(throwable: Throwable?): BackupError {
        return when (throwable) {
            is BackupError -> throwable
            is SerializationException -> BackupError.InvalidJson(throwable.message ?: "Invalid JSON.")
            is FileNotFoundException -> BackupError.FilePermission("File not found: ${throwable.message}")
            is AccessControlException -> BackupError.FilePermission("Permission denied: ${throwable.message}")
            is SecurityException -> BackupError.FilePermission("Permission denied: ${throwable.message}")
            is IOException -> {
                val msg = throwable.message.orEmpty().lowercase()
                when {
                    msg.contains("permission") || msg.contains("eacces") || msg.contains("denied") ->
                        BackupError.FilePermission("Cannot access the selected file.")
                    else -> BackupError.StorageUnavailable("Storage unavailable: ${throwable.message}")
                }
            }
            else -> BackupError.Unknown(throwable?.message ?: "Unknown backup error.")
        }
    }

    /**
     * Builds a timestamped backup file name, e.g. `attendance_tracker_backup_20260802_143000.json`.
     */
    fun createFileName(now: Long = System.currentTimeMillis()): String =
        fileProvider.createDefaultFileName(now)

    /**
     * Returns true when [name] ends with `.json`.
     */
    fun hasJsonExtension(name: String): Boolean = fileProvider.hasJsonExtension(name)
}
