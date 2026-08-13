package com.attendance.tracker.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.attendance.tracker.data.local.database.dao.AttendanceDao
import com.attendance.tracker.data.local.database.dao.ArchiveDao
import com.attendance.tracker.data.local.database.dao.BugReportQueueDao
import com.attendance.tracker.data.local.database.dao.ScheduleDao
import com.attendance.tracker.data.local.database.dao.SemesterDao
import com.attendance.tracker.data.local.database.dao.SubjectDao
import com.attendance.tracker.data.local.database.entity.ArchiveEntity
import com.attendance.tracker.data.local.database.entity.AttendanceEntity
import com.attendance.tracker.data.local.database.entity.ScheduleEntity
import com.attendance.tracker.data.local.database.entity.SemesterVersionEntity
import com.attendance.tracker.data.local.database.entity.SubjectEntity
import com.attendance.tracker.data.local.database.entity.BugReportQueueEntity

/**
 * Main application database constructed with Room.
 * Version 4 adds the durable manual bug-report upload queue.
 */
@Database(
    entities = [
        SubjectEntity::class,
        ScheduleEntity::class,
        SemesterVersionEntity::class,
        AttendanceEntity::class,
        ArchiveEntity::class,
        BugReportQueueEntity::class
    ],
    version = 4,
    exportSchema = true
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

    abstract fun bugReportQueueDao(): BugReportQueueDao
}
