package com.attendance.tracker.feature.bugreport.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.attendance.tracker.feature.bugreport.data.BugReportAttachmentStore
import com.attendance.tracker.feature.bugreport.data.BugReportRetryPolicy
import com.attendance.tracker.feature.bugreport.domain.model.QueuedBugReport
import com.attendance.tracker.feature.bugreport.domain.repository.BugReportQueueRepository
import com.attendance.tracker.feature.bugreport.model.ScreenshotConstraints
import com.attendance.tracker.feature.device.domain.repository.BackendRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class BugReportUploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val queueRepository: BugReportQueueRepository,
    private val backendRepository: BackendRepository,
    private val attachmentStore: BugReportAttachmentStore
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            attachmentStore.cleanupTemporaryFiles()
            queueRepository.resetInFlight()
            while (!isStopped) {
                val report = queueRepository.claimNext() ?: return Result.success()
                when (val result = upload(report)) {
                    UploadResult.Completed -> Unit
                    is UploadResult.Retry -> return result.value
                    UploadResult.PermanentFailure ->
                        return Result.failure(workDataOf(ERROR_KEY to "Permanent upload failure"))
                }
            }
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(workDataOf(ERROR_KEY to "Queue processing failed"))
        }
    }

    private suspend fun upload(report: QueuedBugReport): UploadResult {
        if (report.remoteReportId == null) {
            if (backendRepository.getEnrolledDevice() == null) {
                backendRepository.enrollDevice().getOrElse {
                    return handleFailure(report, it)
                }
            }
            val remoteId = backendRepository.submitBugReport(report.submission).getOrElse {
                return handleFailure(report, it)
            }
            queueRepository.markMetadataUploaded(report.reportId, remoteId)
            if (report.screenshotPath == null) {
                queueRepository.delete(report.reportId)
            }
            return UploadResult.Completed
        }

        val imagePath = report.screenshotPath
        val contentType = report.screenshotContentType
        if (imagePath.isNullOrBlank() || contentType.isNullOrBlank()) {
            queueRepository.markPermanentFailure(report.reportId, "Screenshot metadata is missing")
            return UploadResult.PermanentFailure
        }
        val imageBytes = attachmentStore.read(imagePath)
        if (imageBytes == null) {
            queueRepository.markPermanentFailure(report.reportId, "Screenshot file is missing or invalid")
            return UploadResult.PermanentFailure
        }
        if (contentType !in ScreenshotConstraints.acceptedContentTypes ||
            !ScreenshotConstraints.hasValidSignature(contentType, imageBytes)
        ) {
            queueRepository.markPermanentFailure(report.reportId, "Screenshot file is invalid")
            return UploadResult.PermanentFailure
        }

        backendRepository.uploadScreenshot(
            reportId = report.remoteReportId,
            imageBytes = imageBytes,
            contentType = contentType
        ).getOrElse {
            return handleFailure(report, it)
        }
        queueRepository.delete(report.reportId)
        return UploadResult.Completed
    }

    private suspend fun handleFailure(report: QueuedBugReport, error: Throwable): UploadResult {
        if (!BugReportRetryPolicy.isTransient(error)) {
            queueRepository.markPermanentFailure(
                report.reportId,
                BugReportRetryPolicy.safeReason(error)
            )
            return UploadResult.PermanentFailure
        }

        val nextRetryCount = queueRepository.markRetry(
            report.reportId,
            BugReportRetryPolicy.safeReason(error)
        )
        return if (nextRetryCount >= BugReportRetryPolicy.MAX_RETRIES) {
            queueRepository.markPermanentFailure(
                report.reportId,
                "Maximum upload retries reached"
            )
            UploadResult.PermanentFailure
        } else {
            UploadResult.Retry(Result.retry())
        }
    }

    private sealed interface UploadResult {
        data object Completed : UploadResult
        data object PermanentFailure : UploadResult
        data class Retry(val value: Result) : UploadResult
    }

    private companion object {
        const val ERROR_KEY = "bug_report_upload_error"
    }
}
