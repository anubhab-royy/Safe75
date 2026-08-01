package com.attendance.tracker.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.attendance.tracker.data.local.database.entity.SemesterVersionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Room database operations on academic timetable versions.
 */
@Dao
interface SemesterDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVersion(version: SemesterVersionEntity): Long

    @Update
    suspend fun updateVersion(version: SemesterVersionEntity): Int

    @Delete
    suspend fun deleteVersion(version: SemesterVersionEntity): Int

    @Query("SELECT * FROM semester_versions WHERE id = :id")
    suspend fun getVersion(id: Long): SemesterVersionEntity?

    @Query("SELECT * FROM semester_versions ORDER BY id DESC")
    suspend fun getVersions(): List<SemesterVersionEntity>

    @Query("SELECT * FROM semester_versions ORDER BY id DESC")
    fun observeVersions(): Flow<List<SemesterVersionEntity>>

    @Query("SELECT * FROM semester_versions WHERE isActive = 1 LIMIT 1")
    fun observeActiveVersion(): Flow<SemesterVersionEntity?>

    @Query("SELECT * FROM semester_versions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveVersion(): SemesterVersionEntity?

    @Query("UPDATE semester_versions SET isActive = 0")
    suspend fun deactivateAllVersions()

    @Query("UPDATE semester_versions SET isActive = 1 WHERE id = :versionId")
    suspend fun activateVersion(versionId: Long)

    /**
     * Atomically switches the active timetable version.
     */
    @Transaction
    suspend fun switchActiveVersion(versionId: Long) {
        deactivateAllVersions()
        activateVersion(versionId)
    }
}
