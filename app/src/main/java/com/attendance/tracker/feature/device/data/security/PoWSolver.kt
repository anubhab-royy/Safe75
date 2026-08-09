package com.attendance.tracker.feature.device.data.security

import java.security.MessageDigest

object PoWSolver {

    fun solve(seed: String, difficulty: Int): Long {
        require(difficulty >= 0) { "Difficulty must be non-negative" }
        val digest = MessageDigest.getInstance("SHA-256")
        val targetPrefix = "0".repeat(difficulty)

        var nonce = 0L
        while (true) {
            val input = "$seed$nonce".toByteArray(Charsets.UTF_8)
            val hash = digest.digest(input)
            val hashString = hash.joinToString("") { "%02x".format(it) }
            if (hashString.startsWith(targetPrefix)) {
                return nonce
            }
            nonce++
            if (nonce < 0) {
                throw IllegalStateException("Nonce overflow: exhausted 64-bit range without finding solution")
            }
        }
    }

    fun verify(seed: String, nonce: Long, difficulty: Int): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$seed$nonce".toByteArray(Charsets.UTF_8)
        val hash = digest.digest(input)
        val hashString = hash.joinToString("") { "%02x".format(it) }
        val targetPrefix = "0".repeat(difficulty)
        return hashString.startsWith(targetPrefix)
    }
}
