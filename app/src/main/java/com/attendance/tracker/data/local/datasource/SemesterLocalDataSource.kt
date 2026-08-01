package com.attendance.tracker.data.local.datasource

import com.attendance.tracker.data.local.database.entity.SemesterEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Local Data Source interface defining interactions for semesters.
 */
interface SemesterLocalDataSource {
    /**
     * Exposes a Flow emitting all semesters.
     */
    fun getAllSemesters(): Flow<List<SemesterEntity>>

    /**
     * Inserts or updates a semester entity.
     */
    suspend fun insertSemester(semester: SemesterEntity): Long

    /**
     * Deletes a semester entity.
     */
    suspend fun deleteSemester(semester: SemesterEntity)
}

/**
 * Local Data Source implementation providing skeleton stubs for semester database operations.
 */
class SemesterLocalDataSourceImpl @Inject constructor() : SemesterLocalDataSource {
    override fun getAllSemesters(): Flow<List<SemesterEntity>> = flowOf(emptyList())
    override suspend fun insertSemester(semester: SemesterEntity): Long = 0L
    override suspend fun deleteSemester(semester: SemesterEntity) {}
}
