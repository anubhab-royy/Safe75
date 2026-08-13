package com.attendance.tracker

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.attendance.tracker.core.navigation.AppNavHost
import com.attendance.tracker.core.ui.theme.AttendanceTrackerTheme
import com.attendance.tracker.core.widget.WidgetIntent
import com.attendance.tracker.core.worker.StartupScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single activity entry point for the Safe75 application.
 * Annotated with [AndroidEntryPoint] to enable Hilt dependency injection.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var startupScheduler: StartupScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Request runtime notification permissions on Android 13+ (Tiramisu)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        // The home screen widget can launch the app straight into a destination.
        val widgetDestination = intent.getStringExtra(WidgetIntent.EXTRA_DESTINATION)

        setContent {
            // Reminder/widget scheduling runs after the first frame is drawn and
            // on the IO dispatcher, keeping cold-start work off the critical path.
            LaunchedEffect(Unit) {
                startupScheduler.schedule(applicationContext)
            }

            AttendanceTrackerTheme {
                AppNavHost(startDestination = widgetDestination)
            }
        }
    }
}
