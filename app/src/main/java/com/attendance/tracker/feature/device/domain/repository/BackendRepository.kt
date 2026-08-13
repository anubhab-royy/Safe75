package com.attendance.tracker.feature.device.domain.repository

import com.attendance.tracker.feature.device.domain.model.EnrolledDevice
import com.attendance.tracker.feature.device.domain.model.BugReportSubmission

interface BackendRepository {

    suspend fun checkHealth(): Result<Unit>

    suspend fun requestChallenge(): Result<Pair<String, Int>>

    suspend fun enrollDevice(): Result<EnrolledDevice>

    suspend fun getEnrolledDevice(): EnrolledDevice?

    suspend fun clearEnrollment()

    suspend fun submitBugReport(request: BugReportSubmission): Result<String>

    suspend fun uploadScreenshot(
        reportId: String,
        imageBytes: ByteArray,
        contentType: String
    ): Result<Unit>
}
