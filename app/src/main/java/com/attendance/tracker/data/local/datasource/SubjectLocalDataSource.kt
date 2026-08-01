package com.attendance.tracker.data.local.datasource

import com.attendance.tracker.data.local.database.dao.SubjectDao
import com.attendance.tracker.data.local.database.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Local Data Source interface defining database interactions for study subjects.
 */
interface SubjectLocalDataSource {
    /**
     * Exposes a Flow emitting all stored subject entities.
     */
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    /**
     * Inserts or updates a subject entity.
     */
    suspend fun insertSubject(subject: SubjectEntity): Long

    /**
     * Deletes a subject entity from local storage.
     */
    suspend fun deleteSubject(subject: SubjectEntity)
}

/**
 * Local Data Source implementation wrapping [SubjectDao] operations.
 */
class SubjectLocalDataSourceImpl @Inject constructor(
    private val subjectDao: SubjectDao
) : SubjectLocalDataSource {
    override fun getAllSubjects(): Flow<List<SubjectEntity>> = subjectDao.getAllSubjects()
    override suspend fun insertSubject(subject: SubjectEntity): Long = subjectDao.insertSubject(subject)
    override suspend fun deleteSubject(subject: SubjectEntity) = subjectDao.deleteSubject(subject)
}
