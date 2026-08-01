package com.attendance.tracker.domain.repository

import android.net.Uri
import com.attendance.tracker.core.backup.BackupData
import com.attendance.tracker.core.backup.ValidationResult
import com.attendance.tracker.domain.model.BackupResult
import com.attendance.tracker.domain.model.RestoreOptions
import com.attendance.tracker.domain.model.RestoreResult

/**
 * Repository interface defining all local backup and restore operations.
 */
interface BackupRepository {
    /**
     * Reads the entire database and serializes it to a JSON file at [destinationUri].
     */
    suspend fun exportBackup(destinationUri: Uri): BackupResult

    /**
     * Reads a JSON file at [sourceUri], validates it, and applies the restore
     * according to [options].
     */
    suspend fun importBackup(sourceUri: Uri, options: RestoreOptions): RestoreResult

    /**
     * Reads a JSON file at [sourceUri] and validates its structure and integrity
     * without committing any changes to the database.
     */
    suspend fun validateBackup(sourceUri: Uri): ValidationResult

    /**
     * Reads a JSON file at [sourceUri] and parses it without writing to the database.
     * Used to show a preview before the user confirms the import.
     */
    suspend fun previewBackup(sourceUri: Uri): Result<BackupData>

    /**
     * Takes a snapshot of the current database state as a [BackupData] object.
     */
    suspend fun getBackupData(): BackupData

    /**
     * Applies a pre-validated [BackupData] to the database according to [options].
     */
    suspend fun applyRestore(data: BackupData, options: RestoreOptions): RestoreResult

    /**
     * Returns the epoch-millis timestamp of the last successful backup export,
     * or 0L when no backup has been created yet.
     */
    suspend fun getLastBackupTimestamp(): Long
}
