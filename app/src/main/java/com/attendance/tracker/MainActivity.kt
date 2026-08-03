package com.attendance.tracker

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.attendance.tracker.core.navigation.AppNavHost
import com.attendance.tracker.core.ui.theme.AttendanceTrackerTheme
import com.attendance.tracker.core.widget.WidgetIntent
import com.attendance.tracker.core.widget.WidgetUpdateWorker
import com.attendance.tracker.core.worker.MissedAttendanceWorker
import com.attendance.tracker.core.worker.MorningReminderWorker
import com.attendance.tracker.core.worker.PostClassReminderWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Single activity entry point for the Safe75 application.
 * Annotated with [AndroidEntryPoint] to enable Hilt dependency injection.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Request runtime notification permissions on Android 13+ (Tiramisu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        // Schedule background reminders and widget refreshes
        scheduleReminders()

        // The home screen widget can launch the app straight into a destination.
        val widgetDestination = intent.getStringExtra(WidgetIntent.EXTRA_DESTINATION)

        setContent {
            AttendanceTrackerTheme {
                AppNavHost(startDestination = widgetDestination)
            }
        }
    }

    private fun scheduleReminders() {
        val workManager = WorkManager.getInstance(applicationContext)

        val currentDate = Calendar.getInstance()

        // 1. Morning Schedule Summary Worker (runs daily at 8:00 AM)
        val morningCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        if (morningCal.before(currentDate)) {
            morningCal.add(Calendar.HOUR_OF_DAY, 24)
        }
        val morningDelay = morningCal.timeInMillis - currentDate.timeInMillis
        val morningRequest = PeriodicWorkRequestBuilder<MorningReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(morningDelay, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "morning_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            morningRequest
        )

        // 2. Post-Class Logging Worker (checks finished schedules every 1 hour)
        val postRequest = PeriodicWorkRequestBuilder<PostClassReminderWorker>(1, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork(
            "post_class_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            postRequest
        )

        // 3. End-Of-Day Missed Attendance Worker (runs daily at 9:00 PM)
        val missedCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        if (missedCal.before(currentDate)) {
            missedCal.add(Calendar.HOUR_OF_DAY, 24)
        }
        val missedDelay = missedCal.timeInMillis - currentDate.timeInMillis
        val missedRequest = PeriodicWorkRequestBuilder<MissedAttendanceWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(missedDelay, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "missed_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            missedRequest
        )

        // 4. Daily widget refresh (covers "every WorkManager sync")
        val widgetRefreshRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(24, TimeUnit.HOURS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "widget_refresh_periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            widgetRefreshRequest
        )

        // 5. Immediate widget refresh so the widget is fresh right after launch.
        workManager.enqueueUniqueWork(
            "widget_refresh_startup",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
        )
    }
}
