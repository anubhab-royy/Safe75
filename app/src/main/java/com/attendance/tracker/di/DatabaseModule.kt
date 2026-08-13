package com.attendance.tracker.di

import android.content.Context
import androidx.room.Room
import com.attendance.tracker.data.local.database.AppDatabase
import com.attendance.tracker.data.local.database.Migrations
import com.attendance.tracker.data.local.database.dao.ArchiveDao
import com.attendance.tracker.data.local.database.dao.AttendanceDao
import com.attendance.tracker.data.local.database.dao.BugReportQueueDao
import com.attendance.tracker.data.local.database.dao.ScheduleDao
import com.attendance.tracker.data.local.database.dao.SemesterDao
import com.attendance.tracker.data.local.database.dao.SubjectDao
import com.attendance.tracker.core.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module providing Room Database instances and DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        val builder = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            Constants.DATABASE_NAME
        )
        // Versioned migrations are registered in every build type so existing
        // user data survives upgrades in both Debug and Release.
        builder.addMigrations(*Migrations.ALL)
        if (com.attendance.tracker.BuildConfig.DEBUG) {
            // Dev-only escape hatch: a pre-release development DB whose schema no
            // longer matches any registered migration is rebuilt rather than
            // crashing the debug app. Release intentionally has NO destructive
            // fallback: a missing migration fails the upgrade (data is preserved)
            // instead of silently wiping the database.
            builder.fallbackToDestructiveMigration(true)
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideSubjectDao(database: AppDatabase): SubjectDao {
        return database.subjectDao()
    }

    @Provides
    @Singleton
    fun provideScheduleDao(database: AppDatabase): ScheduleDao {
        return database.scheduleDao()
    }

    @Provides
    @Singleton
    fun provideSemesterDao(database: AppDatabase): SemesterDao {
        return database.semesterDao()
    }

    @Provides
    @Singleton
    fun provideAttendanceDao(database: AppDatabase): AttendanceDao {
        return database.attendanceDao()
    }

    @Provides
    @Singleton
    fun provideArchiveDao(database: AppDatabase): ArchiveDao {
        return database.archiveDao()
    }

    @Provides
    @Singleton
    fun provideBugReportQueueDao(database: AppDatabase): BugReportQueueDao {
        return database.bugReportQueueDao()
    }
}
