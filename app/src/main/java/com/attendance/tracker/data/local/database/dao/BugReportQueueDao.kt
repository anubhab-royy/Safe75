package com.attendance.tracker.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.attendance.tracker.data.local.database.entity.BugReportQueueEntity

@Dao
interface BugReportQueueDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: BugReportQueueEntity): Long

    @Query("SELECT * FROM bug_report_queue WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun findByFingerprint(fingerprint: String): BugReportQueueEntity?

    @Query("SELECT * FROM bug_report_queue WHERE reportId = :reportId LIMIT 1")
    suspend fun findById(reportId: String): BugReportQueueEntity?

    @Query(
        """
        SELECT * FROM bug_report_queue
        WHERE status IN ('QUEUED', 'SCREENSHOT_PENDING', 'UPLOADING_METADATA', 'UPLOADING_SCREENSHOT')
          AND retryCount < :maxRetries
        ORDER BY createdAt ASC
        LIMIT 1
        """
    )
    suspend fun findNextPending(maxRetries: Int): BugReportQueueEntity?

    @Query(
        """
        UPDATE bug_report_queue
        SET status = CASE WHEN remoteReportId IS NULL THEN 'QUEUED' ELSE 'SCREENSHOT_PENDING' END,
            updatedAt = :updatedAt
        WHERE status IN ('UPLOADING_METADATA', 'UPLOADING_SCREENSHOT')
        """
    )
    suspend fun resetInFlight(updatedAt: Long): Int

    @Query(
        """
        UPDATE bug_report_queue
        SET status = 'UPLOADING_METADATA', updatedAt = :updatedAt
        WHERE reportId = :reportId AND status = 'QUEUED'
        """
    )
    suspend fun claimMetadata(reportId: String, updatedAt: Long): Int

    @Query(
        """
        UPDATE bug_report_queue
        SET remoteReportId = :remoteReportId,
            status = CASE WHEN screenshotPath IS NULL THEN 'COMPLETED' ELSE 'SCREENSHOT_PENDING' END,
            updatedAt = :updatedAt,
            lastError = NULL
        WHERE reportId = :reportId AND status = 'UPLOADING_METADATA'
        """
    )
    suspend fun markMetadataUploaded(reportId: String, remoteReportId: String, updatedAt: Long): Int

    @Query(
        """
        UPDATE bug_report_queue
        SET status = 'UPLOADING_SCREENSHOT', updatedAt = :updatedAt
        WHERE reportId = :reportId AND status = 'SCREENSHOT_PENDING'
        """
    )
    suspend fun claimScreenshot(reportId: String, updatedAt: Long): Int

    @Query(
        """
        UPDATE bug_report_queue
        SET status = CASE WHEN remoteReportId IS NULL THEN 'QUEUED' ELSE 'SCREENSHOT_PENDING' END,
            retryCount = retryCount + 1,
            updatedAt = :updatedAt, lastError = :lastError
        WHERE reportId = :reportId
        """
    )
    suspend fun markRetry(reportId: String, lastError: String, updatedAt: Long): Int

    @Query(
        """
        UPDATE bug_report_queue
        SET status = 'FAILED_PERMANENT', updatedAt = :updatedAt, lastError = :lastError
        WHERE reportId = :reportId
        """
    )
    suspend fun markPermanentFailure(reportId: String, lastError: String, updatedAt: Long): Int

    @Query("DELETE FROM bug_report_queue WHERE reportId = :reportId")
    suspend fun delete(reportId: String): Int

    @Query("SELECT COUNT(*) FROM bug_report_queue WHERE status != 'COMPLETED'")
    suspend fun countOutstanding(): Int
}
