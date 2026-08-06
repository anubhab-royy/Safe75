package com.attendance.tracker.backend.security

import com.attendance.tracker.backend.config.EnvironmentConfig
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/**
 * Handles IP and Device ID rate limiting using a token-bucket algorithm and escalates
 * block durations progressively on repeated violations.
 */
object RateLimiter {
    private val ipBuckets = ConcurrentHashMap<String, TokenBucket>()
    private val deviceBuckets = ConcurrentHashMap<String, TokenBucket>()

    private val ipViolations = ConcurrentHashMap<String, Int>()
    private val deviceViolations = ConcurrentHashMap<String, Int>()

    private val blocks = ConcurrentHashMap<String, Long>()

    private val ipLimit: Int
        get() = EnvironmentConfig.get("RATE_LIMIT_IP_MAX", "30").toInt()

    private val deviceLimit: Int
        get() = EnvironmentConfig.get("RATE_LIMIT_DEVICE_MAX", "10").toInt()

    // Default rate: 1 token per 2 seconds, configurable
    private val refillRatePerMs: Double
        get() = 1.0 / EnvironmentConfig.get("RATE_LIMIT_REFILL_PERIOD_MS", "2000").toDouble()

    class TokenBucket(val limit: Int, val refillRatePerMs: Double) {
        private var tokens = limit.toDouble()
        private var lastRefill = System.currentTimeMillis()

        @Synchronized
        fun consume(): Boolean {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRefill
            lastRefill = now
            tokens = min(limit.toDouble(), tokens + elapsed * refillRatePerMs)

            if (tokens >= 1.0) {
                tokens -= 1.0
                return true
            }
            return false
        }
    }

    /**
     * Resets rate limit buckets and violation tracking (mainly for test suites).
     */
    fun reset() {
        ipBuckets.clear()
        deviceBuckets.clear()
        ipViolations.clear()
        deviceViolations.clear()
        blocks.clear()
    }

    /**
     * Returns block remaining duration in seconds, or 0 if not currently blocked.
     */
    fun checkBlock(ip: String, deviceId: String?): Long {
        val now = System.currentTimeMillis()

        val ipBlockExp = blocks[ip]
        if (ipBlockExp != null) {
            if (now < ipBlockExp) return (ipBlockExp - now) / 1000
            blocks.remove(ip)
        }

        if (deviceId != null) {
            val devBlockExp = blocks[deviceId]
            if (devBlockExp != null) {
                if (now < devBlockExp) return (devBlockExp - now) / 1000
                blocks.remove(deviceId)
            }
        }

        return 0L
    }

    /**
     * Acquires rate limit tokens. Returns true if request is allowed, or false if it is throttled.
     */
    fun acquire(ip: String, deviceId: String?): Boolean {
        if (checkBlock(ip, deviceId) > 0L) {
            return false
        }

        val ipBucket = ipBuckets.computeIfAbsent(ip) { TokenBucket(ipLimit, refillRatePerMs) }
        if (!ipBucket.consume()) {
            registerViolation(ip, ipViolations)
            return false
        }

        if (deviceId != null) {
            val devBucket = deviceBuckets.computeIfAbsent(deviceId) { TokenBucket(deviceLimit, refillRatePerMs) }
            if (!devBucket.consume()) {
                registerViolation(deviceId, deviceViolations)
                return false
            }
        }

        return true
    }

    private fun registerViolation(target: String, violationsMap: ConcurrentHashMap<String, Int>) {
        val count = violationsMap.merge(target, 1) { old, new -> old + new } ?: 1
        val cooldownMinutes = when (count) {
            1 -> 1L
            2 -> 5L
            else -> 30L
        }
        blocks[target] = System.currentTimeMillis() + cooldownMinutes * 60 * 1000
    }
}
