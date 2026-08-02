package com.attendance.tracker.core.widget

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * Enqueues a one-shot [WidgetUpdateWorker] so the widget reflects the latest
 * attendance data after every change (marking attendance, restore, reset, etc.).
 *
 * Using [ExistingWorkPolicy.REPLACE] coalesces rapid consecutive triggers into a
 * single refresh.
 */
object WidgetRefreshScheduler {

    private const val UNIQUE_WORK_NAME = "widget_refresh"

    /**
     * Schedules an immediate widget refresh on the WorkManager queue.
     */
    fun schedule(context: Context) {
        val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }
}
