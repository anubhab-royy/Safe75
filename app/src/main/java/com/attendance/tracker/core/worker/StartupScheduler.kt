package com.attendance.tracker.core.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.attendance.tracker.core.common.DispatcherProvider
import com.attendance.tracker.core.widget.WidgetUpdateWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

/**
 * Deferred, idempotent startup scheduling for the reminder and widget workers.
 *
 * Runs after the first frame is drawn and off the main thread so cold-start
 * work is minimized. Every periodic job is enqueued with
 * [ExistingPeriodicWorkPolicy.KEEP], so work that is already scheduled on a
 * previous launch is never duplicated.
 */
@Singleton
class StartupScheduler @Inject constructor(
    private val dispatcherProvider: DispatcherProvider
) {

    /**
     * (Re)enqueues the reminder and widget-refresh jobs if not already present.
     * Suspends onto the IO dispatcher so no WorkManager I/O happens on main.
     */
    suspend fun schedule(context: Context) = withContext(dispatcherProvider.io) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        val now = System.currentTimeMillis()

        // 1. Morning schedule summary — daily at 8:00 AM.
        workManager.enqueueUniquePeriodicWork(
            WORK_MORNING,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<MorningReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(nextOccurrence(now, HOUR_MORNING, 0), TimeUnit.MILLISECONDS)
                .build()
        )

        // 2. Post-class logging — every hour.
        workManager.enqueueUniquePeriodicWork(
            WORK_POST_CLASS,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<PostClassReminderWorker>(1, TimeUnit.HOURS).build()
        )

        // 3. End-of-day missed attendance — daily at 9:00 PM.
        workManager.enqueueUniquePeriodicWork(
            WORK_MISSED,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<MissedAttendanceWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(nextOccurrence(now, HOUR_MISSED, 0), TimeUnit.MILLISECONDS)
                .build()
        )

        // 4. Daily widget refresh (covers "every WorkManager sync").
        workManager.enqueueUniquePeriodicWork(
            WORK_WIDGET_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WidgetUpdateWorker>(24, TimeUnit.HOURS).build()
        )

        // 5. Immediate widget refresh so the widget is fresh right after launch.
        workManager.enqueueUniqueWork(
            WORK_WIDGET_STARTUP,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
        )
    }

    /**
     * Milliseconds from [now] until the next occurrence of [hour]:[minute],
     * rolling to the following day once that time has already passed today.
     * Faithful to the previous `Calendar`-based computation (including the
     * sub-second component) so scheduled run times are unchanged.
     */
    internal fun nextOccurrence(now: Long, hour: Int, minute: Int): Long {
        val due = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        val current = Calendar.getInstance().apply { timeInMillis = now }
        if (due.before(current)) {
            due.add(Calendar.HOUR_OF_DAY, 24)
        }
        return due.timeInMillis - now
    }

    private companion object {
        const val WORK_MORNING = "morning_reminder"
        const val WORK_POST_CLASS = "post_class_reminder"
        const val WORK_MISSED = "missed_reminder"
        const val WORK_WIDGET_PERIODIC = "widget_refresh_periodic"
        const val WORK_WIDGET_STARTUP = "widget_refresh_startup"

        const val HOUR_MORNING = 8
        const val HOUR_MISSED = 21
    }
}
