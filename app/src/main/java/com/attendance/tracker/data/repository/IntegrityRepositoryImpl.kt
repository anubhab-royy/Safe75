package com.attendance.tracker.data.repository

import com.attendance.tracker.data.integrity.DataIntegrityVerifier
import com.attendance.tracker.domain.model.IntegrityReport
import com.attendance.tracker.domain.repository.IntegrityRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [IntegrityRepository] delegating to [DataIntegrityVerifier].
 */
@Singleton
class IntegrityRepositoryImpl @Inject constructor(
    private val verifier: DataIntegrityVerifier
) : IntegrityRepository {

    override suspend fun checkIntegrity(): IntegrityReport {
        return verifier.verify()
    }
}
