package com.attendance.tracker.feature.bugreport.domain.repository

import com.attendance.tracker.feature.bugreport.domain.model.QueueEnqueueResult
import com.attendance.tracker.feature.bugreport.domain.model.QueuedBugReport
import com.attendance.tracker.feature.bugreport.model.ScreenshotAttachment
import com.attendance.tracker.feature.device.domain.model.BugReportSubmission

interface BugReportQueueRepository {
    suspend fun enqueue(
        submission: BugReportSubmission,
        screenshot: ScreenshotAttachment?
    ): Result<QueueEnqueueResult>

    suspend fun resetInFlight()

    suspend fun claimNext(): QueuedBugReport?

    suspend fun markMetadataUploaded(reportId: String, remoteReportId: String)

    suspend fun markRetry(reportId: String, errorMessage: String): Int

    suspend fun markPermanentFailure(reportId: String, errorMessage: String)

    suspend fun delete(reportId: String)

    suspend fun countOutstanding(): Int
}
