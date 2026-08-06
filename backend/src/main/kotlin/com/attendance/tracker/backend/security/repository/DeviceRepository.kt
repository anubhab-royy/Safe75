package com.attendance.tracker.backend.security.repository

import com.attendance.tracker.backend.database.MongoClientProvider
import com.attendance.tracker.backend.security.model.Device
import com.attendance.tracker.backend.security.model.toDevice
import com.attendance.tracker.backend.security.model.toDocument
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull
import org.bson.Document
import org.slf4j.LoggerFactory

interface DeviceRepository {
    suspend fun save(device: Device): Boolean
    suspend fun findById(deviceId: String): Device?
    suspend fun updateLastSeen(deviceId: String): Boolean
    suspend fun createIndexes()
}

/**
 * Concrete MongoDB implementation for managing enrolled device credentials.
 */
class DeviceRepositoryImpl(
    private val database: MongoDatabase = MongoClientProvider.database
) : DeviceRepository {
    private val logger = LoggerFactory.getLogger(DeviceRepositoryImpl::class.java)
    private val collection = database.getCollection<Document>("devices")

    override suspend fun save(device: Device): Boolean {
        return try {
            collection.insertOne(device.toDocument())
            true
        } catch (e: Exception) {
            logger.error("Failed to write device enrollment to MongoDB: ${e.message}")
            false
        }
    }

    override suspend fun findById(deviceId: String): Device? {
        return try {
            val filter = Filters.eq("deviceId", deviceId)
            val doc = collection.find(filter).firstOrNull()
            doc?.toDevice()
        } catch (e: Exception) {
            logger.error("Failed to query device metadata: ${e.message}")
            null
        }
    }

    override suspend fun updateLastSeen(deviceId: String): Boolean {
        return try {
            val filter = Filters.eq("deviceId", deviceId)
            val update = Updates.set("lastSeen", System.currentTimeMillis())
            val result = collection.updateOne(filter, update)
            result.matchedCount > 0
        } catch (e: Exception) {
            logger.error("Failed to update device lastSeen: ${e.message}")
            false
        }
    }

    override suspend fun createIndexes() {
        try {
            logger.info("Creating unique index on devices collection...")
            collection.createIndex(Document("deviceId", 1), IndexOptions().unique(true))
            logger.info("Unique index on 'deviceId' created successfully.")
        } catch (e: Exception) {
            logger.error("Failed to build devices indexes: ${e.message}")
        }
    }
}
