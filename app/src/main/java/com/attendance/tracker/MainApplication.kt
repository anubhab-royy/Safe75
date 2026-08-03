package com.attendance.tracker

import android.app.Application
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.attendance.tracker.core.logger.Logger
import com.attendance.tracker.core.notification.TrackerNotificationManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Main application class responsible for bootstrapping Hilt dependency injection
 * and custom WorkManager initializations.
 */
@HiltAndroidApp
class MainApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
        TrackerNotificationManager.createNotificationChannelsAsync(this)

        // Initialize OpenCV
        if (org.opencv.android.OpenCVLoader.initDebug()) {
            Logger.d("MainApplication", "OpenCV loaded successfully")
        } else {
            Logger.e("MainApplication", "OpenCV initialization failed!")
        }
    }

    /**
     * Enables StrictMode policies in debug builds to surface main-thread I/O
     * and accidental disk writes during development.
     */
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectActivityLeaks()
                .detectLeakedRegistrationObjects()
                .penaltyLog()
                .build()
        )
        Logger.i(TAG, "StrictMode enabled for debug build")
    }

    private companion object {
        const val TAG = "MainApplication"
    }
}
