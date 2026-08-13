package com.attendance.tracker.feature.device.data.di

import com.attendance.tracker.feature.device.data.security.HmacSigner
import com.attendance.tracker.feature.device.data.security.PoWSolver
import com.attendance.tracker.feature.device.data.security.SecureDeviceStorage
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideSecureDeviceStorage(
        @ApplicationContext context: Context
    ): SecureDeviceStorage {
        return SecureDeviceStorage(context)
    }

    @Provides
    @Singleton
    fun providePoWSolver(): PoWSolver {
        return PoWSolver
    }

    @Provides
    @Singleton
    fun provideHmacSigner(): HmacSigner {
        return HmacSigner
    }
}
