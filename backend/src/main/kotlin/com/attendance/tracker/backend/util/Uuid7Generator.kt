package com.attendance.tracker.backend.util

import java.security.SecureRandom
import java.util.UUID

/**
 * Utility to generate time-ordered UUIDv7 identifiers.
 */
object Uuid7Generator {
    private val random = SecureRandom()

    fun generate(): UUID {
        val value = ByteArray(16)
        random.nextBytes(value)

        val timestamp = System.currentTimeMillis()

        // 48-bit timestamp (most significant bits)
        value[0] = (timestamp shr 40).toByte()
        value[1] = (timestamp shr 32).toByte()
        value[2] = (timestamp shr 24).toByte()
        value[3] = (timestamp shr 16).toByte()
        value[4] = (timestamp shr 8).toByte()
        value[5] = timestamp.toByte()

        // Version 7: set bits 4-7 of byte 6 to 0111
        value[6] = (value[6].toInt() and 0x0F or 0x70).toByte()

        // Variant 1: set bits 6-7 of byte 8 to 10 (RFC 4122 variant)
        value[8] = (value[8].toInt() and 0x3F or 0x80).toByte()

        // Construct UUID
        var msb = 0L
        var lsb = 0L
        for (i in 0..7) {
            msb = msb shl 8 or (value[i].toInt() and 0xFF).toLong()
        }
        for (i in 8..15) {
            lsb = lsb shl 8 or (value[i].toInt() and 0xFF).toLong()
        }
        return UUID(msb, lsb)
    }
}
