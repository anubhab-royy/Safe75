package com.attendance.tracker.core.diagnostics

/**
 * Supplies the current [DeviceInfo] snapshot. Platform-free so it can be
 * implemented by an Android reader or faked in tests.
 */
interface DeviceInfoProvider {

    /** Returns the current device and app environment. */
    fun deviceInfo(): DeviceInfo
}
