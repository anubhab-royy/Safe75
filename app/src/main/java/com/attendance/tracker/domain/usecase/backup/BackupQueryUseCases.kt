package com.attendance.tracker.domain.usecase.backup

import android.net.Uri
import com.attendance.tracker.core.backup.BackupData
import com.attendance.tracker.core.backup.ValidationResult
import com.attendance.tracker.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * Validates a backup file at the given [Uri] without writing anything to the database.
 */
class ValidateBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(sourceUri: Uri): ValidationResult {
        return backupRepository.validateBackup(sourceUri)
    }
}

/**
 * Reads a backup file for preview display without writing to the database.
 */
class PreviewBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(sourceUri: Uri): Result<BackupData> {
        return backupRepository.previewBackup(sourceUri)
    }
}

/**
 * Returns a snapshot of the current database as [BackupData] for size estimation or preview.
 */
class GetBackupDataUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(): BackupData {
        return backupRepository.getBackupData()
    }
}
