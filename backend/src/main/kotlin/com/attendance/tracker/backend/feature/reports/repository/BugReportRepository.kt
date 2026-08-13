package com.attendance.tracker.backend.feature.reports.repository

import com.attendance.tracker.backend.database.MongoClientProvider
import com.attendance.tracker.backend.feature.reports.model.BugReport
import com.attendance.tracker.backend.feature.reports.model.UploadStatus
import com.attendance.tracker.backend.feature.reports.model.toBugReport
import com.attendance.tracker.backend.feature.reports.model.toDocument
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document
import org.slf4j.LoggerFactory

interface BugReportRepository {
    suspend fun save(report: BugReport): Boolean
    suspend fun findById(reportId: String): BugReport?
    suspend fun updateScreenshot(reportId: String, objectKey: String?, status: UploadStatus): Boolean
    suspend fun createIndexes()
}

/**
 * Concrete MongoDB repository managing persistence in the "reports" collection.
 */
class BugReportRepositoryImpl(
    private val database: MongoDatabase = MongoClientProvider.database
) : BugReportRepository {
    private val logger = LoggerFactory.getLogger(BugReportRepositoryImpl::class.java)
    private val collection = database.getCollection<Document>("reports")

    override suspend fun save(report: BugReport): Boolean {
        return try {
            val document = report.toDocument()
            collection.insertOne(document)
            true
        } catch (e: Exception) {
            logger.error("Failed to save bug report into MongoDB: ${e.message}")
            false
        }
    }

    override suspend fun findById(reportId: String): BugReport? {
        return try {
            val filter = Filters.eq("reportId", reportId)
            val doc = collection.find(filter).firstOrNull()
            doc?.toBugReport()
        } catch (e: Exception) {
            logger.error("Failed to find bug report $reportId: ${e.message}")
            null
        }
    }

    override suspend fun updateScreenshot(reportId: String, objectKey: String?, status: UploadStatus): Boolean {
        return try {
            val filter = Filters.eq("reportId", reportId)
            val update = Updates.combine(
                Updates.set("screenshotObjectKey", objectKey),
                Updates.set("uploadStatus", status.name),
                Updates.set("updatedAt", System.currentTimeMillis())
            )
            val result = collection.updateOne(filter, update)
            result.matchedCount > 0
        } catch (e: Exception) {
            logger.error("Failed to update screenshot for report $reportId: ${e.message}")
            false
        }
    }

    override suspend fun createIndexes() {
        try {
            logger.info("Creating indexes for collection 'reports'...")
            collection.createIndex(Document("reportId", 1), IndexOptions().unique(true))
            collection.createIndex(Document("createdAt", 1))
            collection.createIndex(Document("uploadStatus", 1))
            logger.info("Successfully created indexes for 'reports'.")
        } catch (e: Exception) {
            logger.error("Failed to build MongoDB indexes: ${e.message}")
        }
    }
}
