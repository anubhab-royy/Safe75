package com.attendance.tracker.feature.bugreport.domain.model

import com.attendance.tracker.feature.device.domain.model.BugReportSubmission

enum class BugReportQueueStatus {
    QUEUED,
    UPLOADING_METADATA,
    SCREENSHOT_PENDING,
    UPLOADING_SCREENSHOT,
    COMPLETED,
    FAILED_PERMANENT
}

data class QueuedBugReport(
    val reportId: String,
    val submission: BugReportSubmission,
    val screenshotPath: String?,
    val screenshotContentType: String?,
    val remoteReportId: String?,
    val status: BugReportQueueStatus,
    val retryCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val lastError: String?
)

data class QueueEnqueueResult(
    val reportId: String,
    val alreadyQueued: Boolean
)
