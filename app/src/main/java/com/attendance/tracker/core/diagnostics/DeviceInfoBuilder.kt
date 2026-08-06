package com.attendance.tracker.core.diagnostics

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure mapper that assembles a [DeviceInfo] from raw OS/app values, normalising
 * null or blank inputs to [UNKNOWN].
 *
 * Kept Android-free so it is trivially unit-testable.
 */
@Singleton
class DeviceInfoBuilder @Inject constructor() {

    fun build(
        appVersion: String?,
        versionCode: Int,
        buildType: String?,
        androidVersion: String?,
        sdkInt: Int,
        manufacturer: String?,
        model: String?,
        brand: String?,
        abi: String?,
        locale: String?,
        densityDpi: Int,
        memoryClassMb: Int
    ): DeviceInfo = DeviceInfo(
        appVersion = appVersion.normalise(),
        versionCode = versionCode,
        buildType = buildType.normalise(),
        androidVersion = androidVersion.normalise(),
        sdkInt = sdkInt,
        manufacturer = manufacturer.normalise(),
        model = model.normalise(),
        brand = brand.normalise(),
        abi = abi.normalise(),
        locale = locale.normalise(),
        densityDpi = densityDpi,
        memoryClassMb = memoryClassMb
    )

    /**
     * Returns a fully unknown [DeviceInfo], used as a safe fallback when the
     * real provider cannot be queried (e.g. during a crash).
     */
    fun unknown(): DeviceInfo = build(
        appVersion = null,
        versionCode = 0,
        buildType = null,
        androidVersion = null,
        sdkInt = 0,
        manufacturer = null,
        model = null,
        brand = null,
        abi = null,
        locale = null,
        densityDpi = 0,
        memoryClassMb = 0
    )

    private fun String?.normalise(): String = this.orEmpty().ifBlank { UNKNOWN }

    companion object {
        const val UNKNOWN = "unknown"
    }
}
