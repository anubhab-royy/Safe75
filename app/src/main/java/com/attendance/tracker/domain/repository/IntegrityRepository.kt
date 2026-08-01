package com.attendance.tracker.domain.repository

import com.attendance.tracker.domain.model.IntegrityReport

/**
 * Repository interface for verifying the referential integrity of the local database.
 */
interface IntegrityRepository {
    /**
     * Scans the database for broken references, duplicate rows, and orphan records.
     *
     * @return An [IntegrityReport] describing every issue found.
     */
    suspend fun checkIntegrity(): IntegrityReport
}
