package com.attendance.tracker.di

import com.attendance.tracker.data.local.datasource.AttendanceLocalDataSource
import com.attendance.tracker.data.local.datasource.AttendanceLocalDataSourceImpl
import com.attendance.tracker.data.local.datasource.ScheduleLocalDataSource
import com.attendance.tracker.data.local.datasource.ScheduleLocalDataSourceImpl
import com.attendance.tracker.data.local.datasource.SemesterLocalDataSource
import com.attendance.tracker.data.local.datasource.SemesterLocalDataSourceImpl
import com.attendance.tracker.data.local.datasource.SettingsLocalDataSource
import com.attendance.tracker.data.local.datasource.SettingsLocalDataSourceImpl
import com.attendance.tracker.data.local.datasource.SubjectLocalDataSource
import com.attendance.tracker.data.local.datasource.SubjectLocalDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module binding Local Data Source interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

    @Binds
    @Singleton
    abstract fun bindSubjectLocalDataSource(
        impl: SubjectLocalDataSourceImpl
    ): SubjectLocalDataSource

    @Binds
    @Singleton
    abstract fun bindAttendanceLocalDataSource(
        impl: AttendanceLocalDataSourceImpl
    ): AttendanceLocalDataSource

    @Binds
    @Singleton
    abstract fun bindScheduleLocalDataSource(
        impl: ScheduleLocalDataSourceImpl
    ): ScheduleLocalDataSource

    @Binds
    @Singleton
    abstract fun bindSemesterLocalDataSource(
        impl: SemesterLocalDataSourceImpl
    ): SemesterLocalDataSource

    @Binds
    @Singleton
    abstract fun bindSettingsLocalDataSource(
        impl: SettingsLocalDataSourceImpl
    ): SettingsLocalDataSource
}
