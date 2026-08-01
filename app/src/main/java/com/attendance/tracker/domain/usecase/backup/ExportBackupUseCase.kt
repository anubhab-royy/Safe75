package com.attendance.tracker.domain.usecase.backup

import android.net.Uri
import com.attendance.tracker.domain.model.BackupResult
import com.attendance.tracker.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * Exports the entire database to a JSON file at the given [Uri].
 */
class ExportBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(destinationUri: Uri): BackupResult {
        return backupRepository.exportBackup(destinationUri)
    }
}
