package com.attendance.tracker.backend.feature.reports.service

import com.attendance.tracker.backend.feature.reports.model.BugReport
import com.attendance.tracker.backend.feature.reports.model.BugReportCreateRequest
import com.attendance.tracker.backend.feature.reports.model.UploadStatus
import com.attendance.tracker.backend.feature.reports.repository.BugReportRepository
import com.attendance.tracker.backend.feature.reports.repository.BugReportRepositoryImpl
import com.attendance.tracker.backend.feature.reports.validation.BugReportValidator
import com.attendance.tracker.backend.storage.R2StorageService
import com.attendance.tracker.backend.util.ImageProcessor
import com.attendance.tracker.backend.util.Uuid7Generator
import org.slf4j.LoggerFactory

/**
 * Service layer coordinating validation, domain model creation, database persistence, and storage operations.
 */
class BugReportService(
    private val repository: BugReportRepository = BugReportRepositoryImpl(),
    private val r2StorageService: R2StorageService = R2StorageService()
) {
    private val logger = LoggerFactory.getLogger(BugReportService::class.java)

    /**
     * Initializes collection indexes.
     */
    suspend fun initializeDb() {
        repository.createIndexes()
    }

    /**
     * Validates the incoming DTO and persists it to the reports database.
     * Generates a time-ordered UUIDv7 report identifier.
     */
    suspend fun createReport(request: BugReportCreateRequest): BugReport {
        logger.info("Validating incoming bug report payload...")
        BugReportValidator.validate(request)

        val reportId = Uuid7Generator.generate().toString()
        val currentTime = System.currentTimeMillis()

        val report = BugReport(
            reportId = reportId,
            timestamp = request.timestamp,
            appVersion = request.appVersion,
            versionCode = request.versionCode,
            buildType = request.buildType,
            androidVersion = request.androidVersion,
            sdkVersion = request.sdkVersion,
            deviceManufacturer = request.deviceManufacturer,
            deviceModel = request.deviceModel,
            cpuAbi = request.cpuAbi,
            locale = request.locale,
            crashReportId = request.crashReportId,
            userDescription = request.userDescription,
            diagnosticsMetadata = request.diagnosticsMetadata,
            uploadStatus = UploadStatus.PENDING,
            retryCount = 0,
            screenshotObjectKey = null,
            createdAt = currentTime,
            updatedAt = currentTime
        )

        logger.info("Persisting report metadata in MongoDB Atlas (Report ID: $reportId)...")
        val success = repository.save(report)
        if (!success) {
            throw RuntimeException("Failed to write report metadata to MongoDB")
        }
        logger.info("Bug report $reportId successfully persisted.")
        return report
    }

    /**
     * Retrieves the stored report metadata corresponding to [reportId].
     */
    suspend fun getReport(reportId: String): BugReport? {
        return repository.findById(reportId)
    }

    /**
     * Processes, validates, downscales, and uploads a screenshot to Cloudflare R2, then updates MongoDB.
     * Enforces size check, format validation, EXIF metadata stripping, duplicate check, and R2 delete rollback.
     */
    suspend fun uploadScreenshot(reportId: String, fileBytes: ByteArray): String {
        logger.info("Screenshot upload started for report: $reportId")

        // 1. Verify report exists
        val report = repository.findById(reportId)
            ?: throw NoSuchElementException("Report with ID $reportId not found")

        // 2. Reject duplicate uploads
        if (report.screenshotObjectKey != null) {
            throw IllegalStateException("A screenshot has already been uploaded for report $reportId")
        }

        // 3. Enforce 2MB size limit
        if (fileBytes.size > 2 * 1024 * 1024) {
            throw IllegalArgumentException("Screenshot exceeds maximum size limit of 2 MB")
        }

        // 4. Validate magic bytes and extract format
        val format = ImageProcessor.detectFormat(fileBytes)
            ?: throw IllegalArgumentException("Unsupported image format. Accepted formats: JPEG, PNG, WEBP.")

        // 5. Downscale, strip metadata and convert to standard JPEG
        val processedBytes = ImageProcessor.processImage(fileBytes)

        // 6. Define deterministic R2 object key
        val objectKey = "reports/$reportId/screenshot.jpg"

        // 7. Upload to Cloudflare R2
        r2StorageService.upload(objectKey, processedBytes, "image/jpeg")

        // 8. Update database metadata
        logger.info("Updating screenshot object key in database for report $reportId...")
        val dbUpdated = repository.updateScreenshot(reportId, objectKey, UploadStatus.SCREENSHOT_UPLOADED)

        if (!dbUpdated) {
            logger.warn("Database metadata update failed for report $reportId. Executing rollback deletion on Cloudflare R2...")
            // ROLLBACK: Delete object from R2 to avoid orphan files
            try {
                r2StorageService.delete(objectKey)
                logger.info("Rollback completed: R2 file deleted successfully.")
            } catch (e: Exception) {
                logger.error("Rollback failed to delete R2 object '$objectKey': ${e.message}")
            }
            throw RuntimeException("Failed to update database metadata with screenshot path")
        }

        logger.info("Screenshot upload and database update completed successfully for report $reportId.")
        return objectKey
    }
}
