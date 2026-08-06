package com.attendance.tracker.backend.database

import com.attendance.tracker.backend.config.EnvironmentConfig
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.bson.Document
import org.slf4j.LoggerFactory

/**
 * Provides database connection clients for MongoDB Atlas using the official Kotlin Coroutines driver.
 */
object MongoClientProvider {
    private val logger = LoggerFactory.getLogger(MongoClientProvider::class.java)

    val client: MongoClient by lazy {
        val uri = EnvironmentConfig.get("MONGODB_URI")
        logger.info("Initializing MongoDB Client...")
        MongoClient.create(uri)
    }

    val database: MongoDatabase by lazy {
        val dbName = EnvironmentConfig.get("DATABASE_NAME", "bug_reports")
        client.getDatabase(dbName)
    }

    /**
     * Verifies connectivity to the MongoDB Atlas cluster by issuing a ping command.
     */
    suspend fun checkConnection(): Boolean {
        return try {
            val dbName = EnvironmentConfig.get("DATABASE_NAME", "bug_reports")
            val pingCommand = Document("ping", 1)
            client.getDatabase(dbName).runCommand(pingCommand)
            true
        } catch (e: Exception) {
            logger.error("MongoDB connection check failed: ${e.message}")
            false
        }
    }
}
