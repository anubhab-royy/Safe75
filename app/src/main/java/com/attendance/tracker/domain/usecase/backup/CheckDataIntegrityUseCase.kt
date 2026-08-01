package com.attendance.tracker.domain.usecase.backup

import com.attendance.tracker.domain.model.IntegrityReport
import com.attendance.tracker.domain.repository.IntegrityRepository
import javax.inject.Inject

/**
 * Scans the local database for data-integrity problems (broken foreign keys,
 * orphan records, and duplicate rows) and returns an [IntegrityReport].
 */
class CheckDataIntegrityUseCase @Inject constructor(
    private val integrityRepository: IntegrityRepository
) {
    suspend operator fun invoke(): IntegrityReport {
        return integrityRepository.checkIntegrity()
    }
}
