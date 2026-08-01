package com.attendance.tracker.domain.usecase.backup

import android.net.Uri
import com.attendance.tracker.domain.model.RestoreOptions
import com.attendance.tracker.domain.model.RestoreResult
import com.attendance.tracker.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * Imports a backup from a JSON file URI and applies selective restore.
 */
class ImportBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(sourceUri: Uri, options: RestoreOptions): RestoreResult {
        return backupRepository.importBackup(sourceUri, options)
    }
}
