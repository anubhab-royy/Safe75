package com.attendance.tracker.core.backup

/**
 * Typed error taxonomy for all backup, restore, and archive operations.
 *
 * Each error carries a user-presentable message so the UI can react
 * specifically (e.g. show "storage unavailable" instead of a raw exception).
 */
sealed class BackupError(message: String) : Exception(message) {

    /** The selected file is not valid, well-formed JSON, or fails structure validation. */
    class InvalidJson(message: String) : BackupError(message)

    /** The backup was created by an older app version and is no longer compatible. */
    class OldBackupVersion(message: String) : BackupError(message)

    /** The backup uses a schema/newer format this app build cannot read. */
    class UnsupportedSchema(message: String) : BackupError(message)

    /** The app could not open or write the selected file (permission/SAT issue). */
    class FilePermission(message: String) : BackupError(message)

    /** No writable storage could be found for the export. */
    class StorageUnavailable(message: String) : BackupError(message)

    /** An archive snapshot could not be decoded (embedded JSON is damaged). */
    class CorruptedArchive(message: String) : BackupError(message)

    /** Any other unexpected failure. */
    class Unknown(message: String) : BackupError(message)
}
