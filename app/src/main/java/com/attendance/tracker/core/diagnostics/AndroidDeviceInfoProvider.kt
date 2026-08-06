package com.attendance.tracker.core.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.attendance.tracker.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android [DeviceInfoProvider] reading hardware, OS, and app metadata.
 *
 * Data is purely technical: model/manufacturer/ABI/OS and build information.
 * No personal identifiers are collected.
 */
@Singleton
class AndroidDeviceInfoProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val builder: DeviceInfoBuilder
) : DeviceInfoProvider {

    override fun deviceInfo(): DeviceInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return builder.build(
            appVersion = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            buildType = if (BuildConfig.DEBUG) BUILD_TYPE_DEBUG else BUILD_TYPE_RELEASE,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            brand = Build.BRAND,
            abi = Build.SUPPORTED_ABIS.firstOrNull(),
            locale = Locale.getDefault().toLanguageTag(),
            densityDpi = context.resources.displayMetrics.densityDpi,
            memoryClassMb = activityManager.memoryClass
        )
    }

    companion object {
        private const val BUILD_TYPE_DEBUG = "debug"
        private const val BUILD_TYPE_RELEASE = "release"
    }
}
