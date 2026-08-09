package com.attendance.tracker.feature.bugreport.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotConstraintsTest {
    @Test
    fun acceptsSupportedImageSignatures() {
        assertTrue(
            ScreenshotConstraints.hasValidSignature(
                "image/jpeg",
                byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
            )
        )
        assertTrue(
            ScreenshotConstraints.hasValidSignature(
                "image/png",
                byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
            )
        )
        assertTrue(
            ScreenshotConstraints.hasValidSignature(
                "image/webp",
                byteArrayOf(0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50)
            )
        )
    }

    @Test
    fun rejectsMismatchedImageSignature() {
        assertFalse(ScreenshotConstraints.hasValidSignature("image/png", byteArrayOf(1, 2, 3)))
        assertFalse(ScreenshotConstraints.hasValidSignature("image/gif", byteArrayOf(1, 2, 3)))
    }
}
