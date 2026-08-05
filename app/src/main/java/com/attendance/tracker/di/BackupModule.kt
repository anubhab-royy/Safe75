package com.attendance.tracker.di

import com.attendance.tracker.data.repository.ArchiveRepositoryImpl
import com.attendance.tracker.data.repository.BackupRepositoryImpl
import com.attendance.tracker.data.repository.IntegrityRepositoryImpl
import com.attendance.tracker.domain.repository.ArchiveRepository
import com.attendance.tracker.domain.repository.BackupRepository
import com.attendance.tracker.domain.repository.IntegrityRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding backup, archive, and integrity repository interfaces
 * to their implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {

    @Binds
    @Singleton
    abstract fun bindBackupRepository(
        impl: BackupRepositoryImpl
    ): BackupRepository

    @Binds
    @Singleton
    abstract fun bindArchiveRepository(
        impl: ArchiveRepositoryImpl
    ): ArchiveRepository

    @Binds
    @Singleton
    abstract fun bindIntegrityRepository(
        impl: IntegrityRepositoryImpl
    ): IntegrityRepository

    companion object {
        @Provides
        @Singleton
        fun provideJson(): kotlinx.serialization.json.Json {
            return kotlinx.serialization.json.Json {
                prettyPrint = true
                encodeDefaults = true
                ignoreUnknownKeys = true
                coerceInputValues = true
            }
        }
    }
}
