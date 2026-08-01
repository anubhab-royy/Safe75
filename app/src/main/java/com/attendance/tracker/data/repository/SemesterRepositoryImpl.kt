package com.attendance.tracker.data.repository

import com.attendance.tracker.data.local.datasource.SemesterLocalDataSource
import com.attendance.tracker.data.mapper.SemesterMapper
import com.attendance.tracker.domain.model.SemesterVersion
import com.attendance.tracker.domain.repository.SemesterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of [SemesterRepository] wrapping local database datasource queries.
 */
class SemesterRepositoryImpl @Inject constructor(
    private val localDataSource: SemesterLocalDataSource
) : SemesterRepository {

    override fun observeVersions(): Flow<List<SemesterVersion>> {
        return localDataSource.observeVersions().map { list ->
            list.map { SemesterMapper.entityToDomain(it) }
        }
    }

    override fun observeActiveVersion(): Flow<SemesterVersion?> {
        return localDataSource.observeActiveVersion().map { entity ->
            entity?.let { SemesterMapper.entityToDomain(it) }
        }
    }

    override suspend fun getVersions(): List<SemesterVersion> {
        return localDataSource.getVersions().map { SemesterMapper.entityToDomain(it) }
    }

    override suspend fun getVersion(id: Long): SemesterVersion? {
        return localDataSource.getVersion(id)?.let { SemesterMapper.entityToDomain(it) }
    }

    override suspend fun getActiveVersion(): SemesterVersion? {
        return localDataSource.getActiveVersion()?.let { SemesterMapper.entityToDomain(it) }
    }

    override suspend fun insertVersion(version: SemesterVersion): Long {
        return localDataSource.insertVersion(SemesterMapper.domainToEntity(version))
    }

    override suspend fun updateVersion(version: SemesterVersion): Int {
        return localDataSource.updateVersion(SemesterMapper.domainToEntity(version))
    }

    override suspend fun deleteVersion(version: SemesterVersion): Int {
        return localDataSource.deleteVersion(SemesterMapper.domainToEntity(version))
    }

    override suspend fun switchActiveVersion(versionId: Long) {
        localDataSource.switchActiveVersion(versionId)
    }
}
