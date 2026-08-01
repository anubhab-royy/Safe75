package com.attendance.tracker.domain.usecase.backup

import com.attendance.tracker.domain.repository.BackupRepository
import javax.inject.Inject

/**
 * Returns the epoch-millis timestamp of the last successful backup export,
 * or 0L when no backup has been created yet.
 */
class GetLastBackupTimestampUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(): Long {
        return backupRepository.getLastBackupTimestamp()
    }
}
