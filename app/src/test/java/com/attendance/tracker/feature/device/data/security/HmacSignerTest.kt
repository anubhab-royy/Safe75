package com.attendance.tracker.feature.device.data.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HmacSignerTest {

    @Test
    fun testSign_producesConsistentHexOutput() {
        val data = "hello world"
        val key = "sec_mysecretkey"

        val sig1 = HmacSigner.sign(data, key)
        val sig2 = HmacSigner.sign(data, key)

        assertEquals(sig1, sig2)
        assertTrue(sig1.matches(Regex("^[0-9a-f]+$")))
        assertTrue(sig1.length == 64)
    }

    @Test
    fun testSign_matchesBackendAlgorithm() {
        val data = "bodycontent"
        val key = "sec_testkey123"

        val androidSig = HmacSigner.sign(data, key)

        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        val secretKey = javax.crypto.spec.SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKey)
        val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        val backendSig = hash.joinToString("") { "%02x".format(it) }

        assertEquals(backendSig, androidSig)
    }

    @Test
    fun testSign_differentData_producesDifferentSignatures() {
        val key = "sec_samekey"
        val sig1 = HmacSigner.sign("data1", key)
        val sig2 = HmacSigner.sign("data2", key)

        assertNotEquals(sig1, sig2)
    }

    @Test
    fun testSign_differentKeys_producesDifferentSignatures() {
        val data = "same data"
        val sig1 = HmacSigner.sign(data, "sec_key1")
        val sig2 = HmacSigner.sign(data, "sec_key2")

        assertNotEquals(sig1, sig2)
    }

    @Test
    fun testGenerateNonce_producesUniqueValues() {
        val nonce1 = HmacSigner.generateNonce()
        val nonce2 = HmacSigner.generateNonce()

        assertNotEquals(nonce1, nonce2)
        assertTrue(nonce1.matches(Regex("^[0-9a-f]+$")))
        assertTrue(nonce1.length == 32)
    }
}
