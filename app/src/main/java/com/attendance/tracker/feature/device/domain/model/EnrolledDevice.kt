package com.attendance.tracker.feature.device.domain.model

data class EnrolledDevice(
    val deviceId: String,
    val deviceSecret: String,
    val createdAt: Long
)

val EnrolledDevice.isEnrolled: Boolean
    get() = deviceId.isNotBlank() && deviceSecret.isNotBlank()
