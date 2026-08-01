package com.attendance.tracker.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.attendance.tracker.data.local.database.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Room database operations on subjects.
 */
@Dao
interface SubjectDao {
    /**
     * Emits all subjects stored in the database as a Flow.
     */
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun observeSubjects(): Flow<List<SubjectEntity>>

    /**
     * Fetches all subjects stored in the database.
     */
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    suspend fun getSubjects(): List<SubjectEntity>

    /**
     * Fetches a specific subject by its unique database ID.
     */
    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubject(id: Long): SubjectEntity?

    /**
     * Searches subjects by name or faculty name.
     */
    @Query("SELECT * FROM subjects WHERE name LIKE '%' || :query || '%' OR (facultyName IS NOT NULL AND facultyName LIKE '%' || :query || '%') ORDER BY name ASC")
    suspend fun searchSubjects(query: String): List<SubjectEntity>

    /**
     * Counts the total number of subjects in the database.
     */
    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun countSubjects(): Int

    /**
     * Inserts a subject in the database.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSubject(subject: SubjectEntity): Long

    /**
     * Updates an existing subject in the database.
     */
    @Update
    suspend fun updateSubject(subject: SubjectEntity): Int

    /**
     * Removes a subject from the database.
     */
    @Delete
    suspend fun deleteSubject(subject: SubjectEntity): Int

    /** Deletes ALL subjects. Used during semester reset when keepSubjects = false. */
    @Query("DELETE FROM subjects")
    suspend fun deleteAllSubjects(): Int

    /** Inserts a subject, replacing on conflict. Used for restore operations. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubject(subject: SubjectEntity): Long
}
