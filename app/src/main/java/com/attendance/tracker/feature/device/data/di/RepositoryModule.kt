package com.attendance.tracker.feature.device.data.di

import com.attendance.tracker.feature.device.data.remote.Safe75ApiService
import com.attendance.tracker.feature.device.data.repository.BackendRepositoryImpl
import com.attendance.tracker.feature.device.data.security.HmacSigner
import com.attendance.tracker.feature.device.data.security.PoWSolver
import com.attendance.tracker.feature.device.data.security.SecureDeviceStorage
import com.attendance.tracker.feature.device.domain.repository.BackendRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideBackendRepository(
        apiService: Safe75ApiService,
        secureStorage: SecureDeviceStorage,
        powSolver: PoWSolver,
        hmacSigner: HmacSigner
    ): BackendRepository {
        return BackendRepositoryImpl(
            apiService = apiService,
            secureStorage = secureStorage,
            powSolver = powSolver,
            hmacSigner = hmacSigner
        )
    }
}
