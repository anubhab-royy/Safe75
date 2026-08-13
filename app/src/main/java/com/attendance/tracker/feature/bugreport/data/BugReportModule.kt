package com.attendance.tracker.feature.bugreport.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.attendance.tracker.feature.bugreport.domain.repository.BugReportQueueRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class BugReportModule {
    @Binds
    @Singleton
    abstract fun bindScreenshotContentReader(
        implementation: AndroidScreenshotContentReader
    ): ScreenshotContentReader

    @Binds
    @Singleton
    abstract fun bindBugReportQueueRepository(
        implementation: BugReportQueueRepositoryImpl
    ): BugReportQueueRepository
}
