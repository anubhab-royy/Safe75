package com.attendance.tracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Main application class responsible for bootstrapping Hilt dependency injection.
 */
@HiltAndroidApp
class MainApplication : Application()
