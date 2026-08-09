package com.attendance.tracker.backend.security

import com.attendance.tracker.backend.config.EnvironmentConfig
import com.attendance.tracker.backend.security.model.DeviceStatus
import com.attendance.tracker.backend.security.repository.DeviceRepository
import com.attendance.tracker.backend.security.repository.DeviceRepositoryImpl
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.plugins.origin
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import org.slf4j.LoggerFactory
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

class SecurityAuthenticationConfig {
    var deviceRepository: DeviceRepository = DeviceRepositoryImpl()
}

/**
 * Custom Ktor plugin enforcing HMAC signature, timestamp drift, nonce reuse, and IP/Device rate limit checks.
 */
val SecurityAuthenticationPlugin = createRouteScopedPlugin(
    name = "SecurityAuthenticationPlugin",
    createConfiguration = ::SecurityAuthenticationConfig
) {
    val logger = LoggerFactory.getLogger("SecurityAuthenticationPlugin")
    val repository = pluginConfig.deviceRepository

    val driftLimitMs = EnvironmentConfig.get("SECURITY_TIMESTAMP_DRIFT_LIMIT_MS", "300000").toLong() // 5 minutes default

    fun getClientIp(call: io.ktor.server.application.ApplicationCall): String {
        return call.request.headers["X-Forwarded-For"]?.split(",")?.firstOrNull()?.trim()
            ?: call.request.origin.remoteHost
    }

    onCall { call ->
        val ip = getClientIp(call)
        val deviceId = call.request.headers["X-Safe75-DeviceId"]
        val signature = call.request.headers["X-Safe75-Signature"]
        val nonce = call.request.headers["X-Safe75-Nonce"]
        val timestampStr = call.request.headers["X-Safe75-Timestamp"]

        // 1. IP & Device Rate limit block checks
        val remainingBlockSeconds = RateLimiter.checkBlock(ip, deviceId)
        if (remainingBlockSeconds > 0L) {
            call.response.headers.append("Retry-After", remainingBlockSeconds.toString())
            call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "Too many requests. Temporary cooldown active."))
            return@onCall
        }

        // 2. Consume rate limiter tokens
        if (!RateLimiter.acquire(ip, deviceId)) {
            logger.warn("Rate limit triggered for IP: $ip, Device: $deviceId")
            call.respond(HttpStatusCode.TooManyRequests, mapOf("error" to "Rate limit exceeded. Please wait."))
            return@onCall
        }

        // 3. Header completeness validation
        if (deviceId.isNullOrBlank() || signature.isNullOrBlank() || nonce.isNullOrBlank() || timestampStr.isNullOrBlank()) {
            logger.warn("Authentication failed: Missing required security headers.")
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing security authentication headers"))
            return@onCall
        }

        val timestamp = timestampStr.toLongOrNull()
        if (timestamp == null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid timestamp format"))
            return@onCall
        }

        // 4. Timestamp drift validation (Replay Protection Phase 1)
        val now = System.currentTimeMillis()
        if (abs(now - timestamp) > driftLimitMs) {
            logger.warn("Replay attack suspected: timestamp drift too high (${abs(now - timestamp)} ms) from device $deviceId")
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Request timestamp is expired or drifts too far from server clock"))
            return@onCall
        }

        // 5. Nonce reuse validation (Replay Protection Phase 2)
        if (!NonceCache.tryAdd(nonce, timestamp)) {
            logger.warn("Replay attack suspected: nonce already used or expired: $nonce")
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid or replayed request nonce"))
            return@onCall
        }

        // 6. Device identity & status validation
        val device = repository.findById(deviceId)
        if (device == null) {
            logger.warn("Authentication failed: Unknown deviceId: $deviceId")
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Device is not enrolled"))
            return@onCall
        }

        if (device.status == DeviceStatus.BLOCKED) {
            logger.warn("Authentication failed: Blocked deviceId: $deviceId attempts request.")
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "This device has been blocked due to policy violations"))
            return@onCall
        }

        // 7. Signature verification (HMAC-SHA256)
        val method = call.request.local.method.value
        val isMultipart = call.request.headers["Content-Type"]?.contains("multipart", ignoreCase = true) == true
        val requestBody = if ((method == "POST" || method == "PUT") && !isMultipart) {
            call.receiveText()
        } else {
            ""
        }

        val dataToSign = "$requestBody$nonce$timestamp"
        val expectedSignature = calculateHmac(dataToSign, device.deviceSecret)

        if (signature != expectedSignature) {
            logger.warn("Authentication failed: Invalid signature from device $deviceId")
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid request signature"))
            return@onCall
        }

        // Authentication succeeded! Track activity
        repository.updateLastSeen(deviceId)
    }
}

/**
 * Calculates HMAC-SHA256 signature for [data] using [key].
 */
fun calculateHmac(data: String, key: String): String {
    val mac = Mac.getInstance("HmacSHA256")
    val secretKey = SecretKeySpec(key.toByteArray(), "HmacSHA256")
    mac.init(secretKey)
    val hash = mac.doFinal(data.toByteArray())
    return hash.joinToString("") { "%02x".format(it) }
}
