package com.attendance.tracker.feature.device.data.security

import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HmacSigner {

    private const val CRYPTO = "HmacSHA256"

    fun sign(data: String, key: String): String {
        val mac = Mac.getInstance(CRYPTO)
        val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), CRYPTO)
        mac.init(secretKey)
        val hash = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun generateNonce(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
