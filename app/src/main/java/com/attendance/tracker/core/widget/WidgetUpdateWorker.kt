package com.attendance.tracker.core.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.attendance.tracker.core.logger.Logger
import com.attendance.tracker.feature.widget.AttendanceWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager worker that refreshes every home screen widget instance.
 *
 * [AttendanceWidget.update] re-runs [AttendanceWidget.provideGlance], which
 * loads a fresh [com.attendance.tracker.feature.widget.WidgetSummary] from the
 * database on a background dispatcher.
 */
@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val widget = AttendanceWidget()
            val manager = GlanceAppWidgetManager(applicationContext)
            manager.getGlanceIds(AttendanceWidget::class.java).forEach { glanceId ->
                widget.update(applicationContext, glanceId)
            }
            Result.success()
        } catch (e: Exception) {
            Logger.e(TAG, "Widget update failed", e)
            Result.retry()
        }
    }

    private companion object {
        const val TAG = "WidgetUpdateWorker"
    }
}
