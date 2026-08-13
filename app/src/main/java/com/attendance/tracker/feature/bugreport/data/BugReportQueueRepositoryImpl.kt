package com.attendance.tracker.feature.bugreport.data

import com.attendance.tracker.core.common.DispatcherProvider
import com.attendance.tracker.data.local.database.dao.BugReportQueueDao
import com.attendance.tracker.data.local.database.entity.BugReportQueueEntity
import com.attendance.tracker.feature.bugreport.domain.model.BugReportQueueStatus
import com.attendance.tracker.feature.bugreport.domain.model.QueueEnqueueResult
import com.attendance.tracker.feature.bugreport.domain.model.QueuedBugReport
import com.attendance.tracker.feature.bugreport.domain.repository.BugReportQueueRepository
import com.attendance.tracker.feature.bugreport.model.ScreenshotAttachment
import com.attendance.tracker.feature.device.domain.model.BugReportSubmission
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BugReportQueueRepositoryImpl @Inject constructor(
    private val dao: BugReportQueueDao,
    private val attachmentStore: BugReportAttachmentStore,
    private val dispatcherProvider: DispatcherProvider
) : BugReportQueueRepository {

    override suspend fun enqueue(
        submission: BugReportSubmission,
        screenshot: ScreenshotAttachment?
    ): Result<QueueEnqueueResult> = runCatching {
        val fingerprint = fingerprint(submission.userDescription, screenshot)
        dao.findByFingerprint(fingerprint)?.let {
            return@runCatching QueueEnqueueResult(it.reportId, alreadyQueued = true)
        }

        val reportId = UUID.randomUUID().toString()
        val screenshotPath = screenshot?.let {
            attachmentStore.write(reportId, it.bytes)
        }
        val now = System.currentTimeMillis()
        val inserted = dao.insert(
            BugReportQueueEntity(
                reportId = reportId,
                fingerprint = fingerprint,
                timestamp = submission.timestamp,
                appVersion = submission.appVersion,
                versionCode = submission.versionCode,
                buildType = submission.buildType,
                androidVersion = submission.androidVersion,
                sdkVersion = submission.sdkVersion,
                deviceManufacturer = submission.deviceManufacturer,
                deviceModel = submission.deviceModel,
                cpuAbi = submission.cpuAbi,
                locale = submission.locale,
                crashReportId = submission.crashReportId,
                userDescription = submission.userDescription,
                diagnosticsMetadata = submission.diagnosticsMetadata,
                screenshotPath = screenshotPath,
                screenshotContentType = screenshot?.contentType,
                remoteReportId = null,
                status = BugReportQueueStatus.QUEUED.name,
                retryCount = 0,
                createdAt = now,
                updatedAt = now,
                lastError = null
            )
        )
        if (inserted == -1L) {
            attachmentStore.delete(screenshotPath)
            val existing = dao.findByFingerprint(fingerprint)
                ?: error("Queue insert conflict without an existing report")
            QueueEnqueueResult(existing.reportId, alreadyQueued = true)
        } else {
            QueueEnqueueResult(reportId, alreadyQueued = false)
        }
    }

    override suspend fun resetInFlight() {
        dao.resetInFlight(System.currentTimeMillis())
    }

    override suspend fun claimNext(): QueuedBugReport? {
        while (true) {
            val candidate = dao.findNextPending(MAX_RETRIES) ?: return null
            val claimed = when (candidate.status) {
                BugReportQueueStatus.QUEUED.name ->
                    dao.claimMetadata(candidate.reportId, System.currentTimeMillis())
                BugReportQueueStatus.SCREENSHOT_PENDING.name ->
                    dao.claimScreenshot(candidate.reportId, System.currentTimeMillis())
                else -> 0
            }
            if (claimed == 1) {
                return toDomain(dao.findById(candidate.reportId) ?: return null)
            }
        }
    }

    override suspend fun markMetadataUploaded(reportId: String, remoteReportId: String) {
        dao.markMetadataUploaded(reportId, remoteReportId, System.currentTimeMillis())
    }

    override suspend fun markRetry(reportId: String, errorMessage: String): Int {
        val entity = dao.findById(reportId) ?: return MAX_RETRIES
        dao.markRetry(reportId, errorMessage, System.currentTimeMillis())
        return entity.retryCount + 1
    }

    override suspend fun markPermanentFailure(reportId: String, errorMessage: String) {
        dao.markPermanentFailure(reportId, errorMessage, System.currentTimeMillis())
    }

    override suspend fun delete(reportId: String) {
        val entity = dao.findById(reportId)
        if (dao.delete(reportId) == 1) {
            attachmentStore.delete(entity?.screenshotPath)
        }
    }

    override suspend fun countOutstanding(): Int = dao.countOutstanding()

    private fun toDomain(entity: BugReportQueueEntity): QueuedBugReport = QueuedBugReport(
        reportId = entity.reportId,
        submission = BugReportSubmission(
            timestamp = entity.timestamp,
            appVersion = entity.appVersion,
            versionCode = entity.versionCode,
            buildType = entity.buildType,
            androidVersion = entity.androidVersion,
            sdkVersion = entity.sdkVersion,
            deviceManufacturer = entity.deviceManufacturer,
            deviceModel = entity.deviceModel,
            cpuAbi = entity.cpuAbi,
            locale = entity.locale,
            crashReportId = entity.crashReportId,
            userDescription = entity.userDescription,
            diagnosticsMetadata = entity.diagnosticsMetadata
        ),
        screenshotPath = entity.screenshotPath,
        screenshotContentType = entity.screenshotContentType,
        remoteReportId = entity.remoteReportId,
        status = BugReportQueueStatus.valueOf(entity.status),
        retryCount = entity.retryCount,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        lastError = entity.lastError
    )

    private suspend fun fingerprint(description: String, screenshot: ScreenshotAttachment?): String =
        withContext(dispatcherProvider.default) {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(description.trim().toByteArray())
            screenshot?.let {
                digest.update(it.contentType.toByteArray())
                digest.update(it.bytes)
            }
            digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        }

    private companion object {
        const val MAX_RETRIES = 5
    }
}
