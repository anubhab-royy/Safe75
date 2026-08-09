package com.attendance.tracker.feature.device.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PoWSolverTest {

    @Test
    fun testSolveAndVerify_difficulty0_returnsZero() {
        val seed = "abc123"
        val nonce = PoWSolver.solve(seed, 0)
        assertEquals(0L, nonce)
        assertTrue(PoWSolver.verify(seed, nonce, 0))
    }

    @Test
    fun testSolveAndVerify_difficulty1_validatesHash() {
        val seed = "testseed"
        val nonce = PoWSolver.solve(seed, 1)
        assertTrue(PoWSolver.verify(seed, nonce, 1))
    }

    @Test
    fun testVerify_invalidNonce_returnsFalse() {
        val seed = "testseed"
        assertFalse(PoWSolver.verify(seed, 999999L, 4))
    }

    @Test
    fun testSolve_matchesBackendAlgorithm() {
        val seed = "aabbccdd"
        val difficulty = 2
        val nonce = PoWSolver.solve(seed, difficulty)
        assertTrue(PoWSolver.verify(seed, nonce, difficulty))

        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val input = "$seed$nonce".toByteArray()
        val hash = digest.digest(input).joinToString("") { "%02x".format(it) }
        assertTrue(hash.startsWith("0".repeat(difficulty)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testSolve_negativeDifficulty_throws() {
        PoWSolver.solve("seed", -1)
    }

    private fun assertFalse(condition: Boolean) {
        assertTrue(!condition)
    }
}
