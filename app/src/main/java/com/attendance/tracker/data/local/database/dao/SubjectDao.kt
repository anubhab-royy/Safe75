package com.attendance.tracker.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.attendance.tracker.data.local.database.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Room database operations on subjects.
 */
@Dao
interface SubjectDao {
    /**
     * Emits all subjects stored in the database.
     */
    @Query("SELECT * FROM subjects")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    /**
     * Inserts or updates a subject in the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    /**
     * Removes a subject from the database.
     */
    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)
}
