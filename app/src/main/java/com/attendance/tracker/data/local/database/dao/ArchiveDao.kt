package com.attendance.tracker.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.attendance.tracker.data.local.database.entity.ArchiveEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for archived semester snapshots.
 */
@Dao
interface ArchiveDao {

    /**
     * Inserts a new archive snapshot.
     * @return The row ID of the inserted archive.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArchive(archive: ArchiveEntity): Long

    /**
     * Observes all archived semesters ordered by most recently archived first.
     */
    @Query("SELECT * FROM archives ORDER BY archivedAt DESC")
    fun observeArchives(): Flow<List<ArchiveEntity>>

    /**
     * Returns all archived semesters as a one-shot list.
     */
    @Query("SELECT * FROM archives ORDER BY archivedAt DESC")
    suspend fun getAllArchives(): List<ArchiveEntity>

    /**
     * Returns a single archive by its primary key, or null if not found.
     */
    @Query("SELECT * FROM archives WHERE id = :id LIMIT 1")
    suspend fun getArchiveById(id: Long): ArchiveEntity?

    /**
     * Deletes a single archive by its primary key.
     * @return The number of rows deleted.
     */
    @Query("DELETE FROM archives WHERE id = :id")
    suspend fun deleteArchiveById(id: Long): Int

    /**
     * Deletes all archived semesters.
     */
    @Query("DELETE FROM archives")
    suspend fun deleteAllArchives()
}
