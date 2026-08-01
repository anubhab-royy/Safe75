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
    fun observeSubjects(): Flow<List<SubjectEntity>>

    /**
     * Fetches all subjects stored in the database.
     */
    suspend fun getSubjects(): List<SubjectEntity>

    /**
     * Fetches a specific subject by its unique database ID.
     */
    suspend fun getSubject(id: Long): SubjectEntity?

    /**
     * Searches subjects by name or faculty name.
     */
    suspend fun searchSubjects(query: String): List<SubjectEntity>

    /**
     * Counts the total number of subjects in the database.
     */
    suspend fun countSubjects(): Int

    /**
     * Inserts a subject entity.
     */
    suspend fun insertSubject(subject: SubjectEntity): Long

    /**
     * Updates an existing subject entity.
     */
    suspend fun updateSubject(subject: SubjectEntity): Int

    /**
     * Deletes a subject entity from local storage.
     */
    suspend fun deleteSubject(subject: SubjectEntity): Int
}

/**
 * Local Data Source implementation wrapping [SubjectDao] operations.
 */
class SubjectLocalDataSourceImpl @Inject constructor(
    private val subjectDao: SubjectDao
) : SubjectLocalDataSource {
    override fun observeSubjects(): Flow<List<SubjectEntity>> = subjectDao.observeSubjects()
    override suspend fun getSubjects(): List<SubjectEntity> = subjectDao.getSubjects()
    override suspend fun getSubject(id: Long): SubjectEntity? = subjectDao.getSubject(id)
    override suspend fun searchSubjects(query: String): List<SubjectEntity> = subjectDao.searchSubjects(query)
    override suspend fun countSubjects(): Int = subjectDao.countSubjects()
    override suspend fun insertSubject(subject: SubjectEntity): Long = subjectDao.insertSubject(subject)
    override suspend fun updateSubject(subject: SubjectEntity): Int = subjectDao.updateSubject(subject)
    override suspend fun deleteSubject(subject: SubjectEntity): Int = subjectDao.deleteSubject(subject)
}
