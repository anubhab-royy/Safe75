package com.attendance.tracker.backend.storage

import com.attendance.tracker.backend.config.EnvironmentConfig
import org.slf4j.LoggerFactory
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

/**
 * Handles binary storage operations on Cloudflare R2 via AWS S3 SDK.
 */
class R2StorageService(
    private val s3Client: S3Client = R2ClientProvider.s3Client
) {
    private val logger = LoggerFactory.getLogger(R2StorageService::class.java)
    private val bucketName = EnvironmentConfig.get("R2_BUCKET")

    /**
     * Uploads [data] byte array to R2 bucket with the specified [key] and [contentType].
     */
    fun upload(key: String, data: ByteArray, contentType: String) {
        try {
            logger.info("Uploading file binary to Cloudflare R2: '$key'...")
            val request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build()

            s3Client.putObject(request, RequestBody.fromBytes(data))
            logger.info("Successfully uploaded object to R2 with key '$key'.")
        } catch (e: Exception) {
            logger.error("Failed to upload object '$key' to Cloudflare R2: ${e.message}")
            throw e
        }
    }

    /**
     * Deletes the object specified by [key] from R2 bucket.
     */
    fun delete(key: String) {
        try {
            logger.info("Removing file from Cloudflare R2: '$key'...")
            val request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build()

            s3Client.deleteObject(request)
            logger.info("Successfully deleted object '$key' from R2.")
        } catch (e: Exception) {
            logger.error("Failed to delete object '$key' from Cloudflare R2: ${e.message}")
            throw e
        }
    }
}
