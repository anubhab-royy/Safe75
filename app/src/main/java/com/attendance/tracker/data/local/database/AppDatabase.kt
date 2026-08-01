package com.attendance.tracker.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.attendance.tracker.data.local.database.dao.SubjectDao
import com.attendance.tracker.data.local.database.entity.SubjectEntity

/**
 * Main application database constructed with Room.
 */
@Database(
    entities = [SubjectEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Exposes Room Subject database operations.
     */
    abstract fun subjectDao(): SubjectDao
}
