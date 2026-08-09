package com.attendance.tracker.feature.bugreport.data

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.attendance.tracker.feature.bugreport.worker.BugReportUploadWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BugReportWorkScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    fun enqueuePendingUploads() {
        val request = OneTimeWorkRequestBuilder<BugReportUploadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                INITIAL_BACKOFF_SECONDS,
                TimeUnit.SECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        const val UNIQUE_WORK_NAME = "bug_report_uploads"
        const val INITIAL_BACKOFF_SECONDS = 30L
    }
}
