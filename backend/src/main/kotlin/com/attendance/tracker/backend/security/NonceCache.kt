package com.attendance.tracker.backend.security

import com.attendance.tracker.backend.config.EnvironmentConfig
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance sliding-window Nonce Cache preventing request replay attacks.
 */
object NonceCache {
    private val nonces = ConcurrentHashMap<String, Long>()

    private val nonceTtlMs: Long
        get() = EnvironmentConfig.get("SECURITY_NONCE_TTL_MS", "600000").toLong()

    /**
     * Clears the cache memory (for testing purposes).
     */
    fun reset() {
        nonces.clear()
    }

    /**
     * Attempts to register [nonce]. Returns true if the nonce is new and unique,
     * or false if it is a replay (already used) or has drifted past the freshness TTL window.
     */
    fun tryAdd(nonce: String, timestamp: Long): Boolean {
        val now = System.currentTimeMillis()

        // Housekeeping: remove nonces that have aged out of the sliding TTL window
        nonces.entries.removeIf { now - it.value > nonceTtlMs }

        // Fail-fast if the client-submitted timestamp is already older than the TTL limit
        if (now - timestamp > nonceTtlMs) {
            return false
        }

        return nonces.putIfAbsent(nonce, timestamp) == null
    }
}
