package com.attendance.tracker.di

import com.attendance.tracker.data.repository.AttendanceRepositoryImpl
import com.attendance.tracker.data.repository.ScheduleRepositoryImpl
import com.attendance.tracker.data.repository.SemesterRepositoryImpl
import com.attendance.tracker.data.repository.SettingsRepositoryImpl
import com.attendance.tracker.data.repository.SubjectRepositoryImpl
import com.attendance.tracker.domain.repository.AttendanceRepository
import com.attendance.tracker.domain.repository.ScheduleRepository
import com.attendance.tracker.domain.repository.SemesterRepository
import com.attendance.tracker.domain.repository.SettingsRepository
import com.attendance.tracker.domain.repository.SubjectRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module binding Repository interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSubjectRepository(
        impl: SubjectRepositoryImpl
    ): SubjectRepository

    @Binds
    @Singleton
    abstract fun bindAttendanceRepository(
        impl: AttendanceRepositoryImpl
    ): AttendanceRepository

    @Binds
    @Singleton
    abstract fun bindScheduleRepository(
        impl: ScheduleRepositoryImpl
    ): ScheduleRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindSemesterRepository(
        impl: SemesterRepositoryImpl
    ): SemesterRepository
}
