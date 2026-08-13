package com.attendance.tracker.backend.storage

import com.attendance.tracker.backend.config.EnvironmentConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import java.net.URI

/**
 * Initializes and manages the S3-compatible client for Cloudflare R2 binary storage.
 */
object R2ClientProvider {
    private val logger = LoggerFactory.getLogger(R2ClientProvider::class.java)

    val s3Client: S3Client by lazy {
        logger.info("Initializing Cloudflare R2 S3 Client...")
        val accessKey = EnvironmentConfig.get("R2_ACCESS_KEY")
        val secretKey = EnvironmentConfig.get("R2_SECRET_KEY")
        val endpoint = EnvironmentConfig.get("R2_ENDPOINT")

        val credentials = AwsBasicCredentials.create(accessKey, secretKey)
        S3Client.builder()
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .endpointOverride(URI.create(endpoint))
            .region(Region.US_EAST_1) // Cloudflare R2 ignores region but AWS SDK requires one, US_EAST_1 is default
            .build()
    }

    /**
     * Verifies connectivity to Cloudflare R2 by heading the target bucket.
     */
    suspend fun checkConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val bucketName = EnvironmentConfig.get("R2_BUCKET")
            val request = HeadBucketRequest.builder().bucket(bucketName).build()
            s3Client.headBucket(request)
            true
        } catch (e: Exception) {
            logger.error("Cloudflare R2 connection check failed: ${e.message}")
            false
        }
    }
}
