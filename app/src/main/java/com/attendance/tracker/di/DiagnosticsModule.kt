package com.attendance.tracker.di

import android.content.Context
import com.attendance.tracker.BuildConfig
import com.attendance.tracker.core.diagnostics.AndroidDeviceInfoProvider
import com.attendance.tracker.core.diagnostics.DiagnosticsFileStorage
import com.attendance.tracker.core.diagnostics.DiagnosticsStorage
import com.attendance.tracker.core.diagnostics.DeviceInfoProvider
import com.attendance.tracker.core.diagnostics.LogBuffer
import com.attendance.tracker.core.diagnostics.RecentLogBuffer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Hilt module wiring the diagnostics framework. The [Json] instance is shared
 * from [BackupModule]; file and scope providers live here.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DiagnosticsModule {

    @Binds
    @Singleton
    abstract fun bindLogBuffer(impl: RecentLogBuffer): LogBuffer

    @Binds
    @Singleton
    abstract fun bindDiagnosticsStorage(impl: DiagnosticsFileStorage): DiagnosticsStorage

    @Binds
    @Singleton
    abstract fun bindDeviceInfoProvider(impl: AndroidDeviceInfoProvider): DeviceInfoProvider

    companion object {
        @Provides
        @Singleton
        fun provideDiagnosticsDirectory(@ApplicationContext context: Context): File =
            File(context.filesDir, "diagnostics")

        @Provides
        @Singleton
        fun provideDiagnosticsScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.IO)

        @Provides
        @Singleton
        fun provideLogcatEnabled(): Boolean = BuildConfig.DEBUG
    }
}
