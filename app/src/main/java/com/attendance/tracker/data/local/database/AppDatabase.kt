package com.attendance.tracker.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.attendance.tracker.data.local.database.dao.AttendanceDao
import com.attendance.tracker.data.local.database.dao.ArchiveDao
import com.attendance.tracker.data.local.database.dao.ScheduleDao
import com.attendance.tracker.data.local.database.dao.SemesterDao
import com.attendance.tracker.data.local.database.dao.SubjectDao
import com.attendance.tracker.data.local.database.entity.ArchiveEntity
import com.attendance.tracker.data.local.database.entity.AttendanceEntity
import com.attendance.tracker.data.local.database.entity.ScheduleEntity
import com.attendance.tracker.data.local.database.entity.SemesterVersionEntity
import com.attendance.tracker.data.local.database.entity.SubjectEntity

/**
 * Main application database constructed with Room.
 * Version 2 adds the [ArchiveEntity] table for semester archiving.
 */
@Database(
    entities = [
        SubjectEntity::class,
        ScheduleEntity::class,
        SemesterVersionEntity::class,
        AttendanceEntity::class,
        ArchiveEntity::class
    ],
    version = 3,
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

    /**
     * Exposes Room Attendance database operations.
     */
    abstract fun attendanceDao(): AttendanceDao

    /**
     * Exposes Room Archive database operations.
     */
    abstract fun archiveDao(): ArchiveDao
}

