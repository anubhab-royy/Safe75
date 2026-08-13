package com.attendance.tracker.backend.security

import com.attendance.tracker.backend.config.EnvironmentConfig
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages Proof-of-Work puzzle challenges to secure the enrollment pipeline.
 */
object ChallengeManager {
    private val secureRandom = SecureRandom()
    private val challengeCache = ConcurrentHashMap<String, Long>()

    val difficulty: Int
        get() = EnvironmentConfig.get("POW_DIFFICULTY", "4").toInt()

    /**
     * Generates a 32-character hex seed that expires in 5 minutes.
     */
    fun generateChallenge(): String {
        val now = System.currentTimeMillis()
        challengeCache.entries.removeIf { now > it.value }

        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        val seed = bytes.joinToString("") { "%02x".format(it) }

        challengeCache[seed] = now + 5 * 60 * 1000 // 5 minutes TTL
        return seed
    }

    /**
     * Verifies that the client solved the challenge by finding a nonce that yields
     * a SHA-256 hash starting with the configured number of zero hexadecimal characters.
     */
    fun verifyChallenge(seed: String, nonce: Long): Boolean {
        val expiration = challengeCache.remove(seed) ?: return false
        if (System.currentTimeMillis() > expiration) {
            return false
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$seed$nonce".toByteArray()
        val hashBytes = digest.digest(input)
        val hashString = hashBytes.joinToString("") { "%02x".format(it) }

        val targetPrefix = "0".repeat(difficulty)
        return hashString.startsWith(targetPrefix)
    }
}
