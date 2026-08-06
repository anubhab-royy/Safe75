package com.attendance.tracker.core.diagnostics

import kotlinx.serialization.Serializable

/**
 * Immutable snapshot of the device and app environment at the time of capture.
 *
 * Fields are strictly technical (hardware, OS, app version). No identifiers
 * that could be tied to a person are collected.
 */
@Serializable
data class DeviceInfo(
    val appVersion: String,
    val versionCode: Int,
    val buildType: String,
    val androidVersion: String,
    val sdkInt: Int,
    val manufacturer: String,
    val model: String,
    val brand: String,
    val abi: String,
    val locale: String,
    val densityDpi: Int,
    val memoryClassMb: Int
)
