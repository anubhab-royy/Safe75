package com.attendance.tracker.domain.repository

import com.attendance.tracker.domain.model.Subject
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining data operations for study subjects.
 */
interface SubjectRepository {
    /**
     * Exposes a Flow emitting all configured subjects.
     */
    fun observeSubjects(): Flow<List<Subject>>

    /**
     * Fetches all subjects stored in the local storage.
     */
    suspend fun getSubjects(): List<Subject>

    /**
     * Fetches a specific subject by its unique database ID.
     */
    suspend fun getSubjectById(id: Long): Subject?

    /**
     * Inserts or updates a subject, returning its database ID.
     */
    suspend fun insertSubject(subject: Subject): Long

    /**
     * Updates an existing subject.
     */
    suspend fun updateSubject(subject: Subject): Int

    /**
     * Removes a subject from the local store.
     */
    suspend fun deleteSubject(subject: Subject): Int

    /**
     * Searches subjects matching the specified query.
     */
    suspend fun searchSubjects(query: String): List<Subject>

    /**
     * Counts the total number of subjects.
     */
    suspend fun countSubjects(): Int
}
