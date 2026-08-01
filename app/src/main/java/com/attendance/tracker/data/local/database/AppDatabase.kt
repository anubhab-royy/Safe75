package com.attendance.tracker.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.attendance.tracker.data.local.database.dao.ScheduleDao
import com.attendance.tracker.data.local.database.dao.SemesterDao
import com.attendance.tracker.data.local.database.dao.SubjectDao
import com.attendance.tracker.data.local.database.entity.ScheduleEntity
import com.attendance.tracker.data.local.database.entity.SemesterVersionEntity
import com.attendance.tracker.data.local.database.entity.SubjectEntity

/**
 * Main application database constructed with Room.
 */
@Database(
    entities = [
        SubjectEntity::class,
        ScheduleEntity::class,
        SemesterVersionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Exposes Room Subject database operations.
     */
    abstract fun subjectDao(): SubjectDao

    /**
     * Exposes Room Schedule database operations.
     */
    abstract fun scheduleDao(): ScheduleDao

    /**
     * Exposes Room Semester Version database operations.
     */
    abstract fun semesterDao(): SemesterDao
}
