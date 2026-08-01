package com.attendance.tracker.domain.repository

import com.attendance.tracker.domain.model.Semester
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface defining operations for academic semester configurations.
 */
interface SemesterRepository {
    /**
     * Exposes a Flow emitting all configured semesters.
     */
    fun getSemesters(): Flow<List<Semester>>

    /**
     * Inserts or updates a semester, returning its database ID.
     */
    suspend fun insertSemester(semester: Semester): Long

    /**
     * Deletes a semester config.
     */
    suspend fun deleteSemester(semester: Semester)
}
