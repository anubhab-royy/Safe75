package com.attendance.tracker.data.local.datasource

import com.attendance.tracker.data.local.database.dao.SemesterDao
import com.attendance.tracker.data.local.database.entity.SemesterVersionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Local Data Source interface defining interactions for semester versions.
 */
interface SemesterLocalDataSource {
    fun observeVersions(): Flow<List<SemesterVersionEntity>>
    fun observeActiveVersion(): Flow<SemesterVersionEntity?>
    suspend fun getVersions(): List<SemesterVersionEntity>
    suspend fun getVersion(id: Long): SemesterVersionEntity?
    suspend fun getActiveVersion(): SemesterVersionEntity?
    suspend fun insertVersion(version: SemesterVersionEntity): Long
    suspend fun updateVersion(version: SemesterVersionEntity): Int
    suspend fun deleteVersion(version: SemesterVersionEntity): Int
    suspend fun switchActiveVersion(versionId: Long)
}

/**
 * Local Data Source implementation providing semester database operations.
 */
class SemesterLocalDataSourceImpl @Inject constructor(
    private val semesterDao: SemesterDao
) : SemesterLocalDataSource {
    override fun observeVersions(): Flow<List<SemesterVersionEntity>> = semesterDao.observeVersions()
    override fun observeActiveVersion(): Flow<SemesterVersionEntity?> = semesterDao.observeActiveVersion()
    override suspend fun getVersions(): List<SemesterVersionEntity> = semesterDao.getVersions()
    override suspend fun getVersion(id: Long): SemesterVersionEntity? = semesterDao.getVersion(id)
    override suspend fun getActiveVersion(): SemesterVersionEntity? = semesterDao.getActiveVersion()
    override suspend fun insertVersion(version: SemesterVersionEntity): Long = semesterDao.insertVersion(version)
    override suspend fun updateVersion(version: SemesterVersionEntity): Int = semesterDao.updateVersion(version)
    override suspend fun deleteVersion(version: SemesterVersionEntity): Int = semesterDao.deleteVersion(version)
    override suspend fun switchActiveVersion(versionId: Long) = semesterDao.switchActiveVersion(versionId)
}
