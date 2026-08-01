package com.attendance.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.attendance.tracker.core.ui.theme.AttendanceTrackerTheme
import com.attendance.tracker.core.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity entry point for the Attendance Tracker application.
 * Annotated with [AndroidEntryPoint] to enable Hilt dependency injection.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the AndroidX Splash Screen before super.onCreate()
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        setContent {
            AttendanceTrackerTheme {
                AppNavHost()
            }
        }
    }
}
