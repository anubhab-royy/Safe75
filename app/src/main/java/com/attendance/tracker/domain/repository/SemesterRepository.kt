package com.attendance.tracker.domain.repository

import com.attendance.tracker.domain.model.SemesterVersion
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining operations for academic semester versions.
 */
interface SemesterRepository {
    fun observeVersions(): Flow<List<SemesterVersion>>
    fun observeActiveVersion(): Flow<SemesterVersion?>
    suspend fun getVersions(): List<SemesterVersion>
    suspend fun getVersion(id: Long): SemesterVersion?
    suspend fun getActiveVersion(): SemesterVersion?
    suspend fun insertVersion(version: SemesterVersion): Long
    suspend fun updateVersion(version: SemesterVersion): Int
    suspend fun deleteVersion(version: SemesterVersion): Int
    suspend fun switchActiveVersion(versionId: Long)
}
