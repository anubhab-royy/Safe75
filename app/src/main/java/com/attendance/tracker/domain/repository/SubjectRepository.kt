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
    fun getSubjects(): Flow<List<Subject>>

    /**
     * Fetches a specific subject by its unique database ID.
     */
    suspend fun getSubjectById(id: Long): Subject?

    /**
     * Inserts or updates a subject, returning its database ID.
     */
    suspend fun insertSubject(subject: Subject): Long

    /**
     * Removes a subject from the local store.
     */
    suspend fun deleteSubject(subject: Subject)
}
