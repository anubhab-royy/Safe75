package com.attendance.tracker.feature.bugreport.model

data class ScreenshotAttachment(
    val uri: String,
    val bytes: ByteArray,
    val contentType: String
)

object ScreenshotConstraints {
    const val MAX_BYTES = 2 * 1024 * 1024
    val acceptedContentTypes = setOf("image/jpeg", "image/png", "image/webp")

    fun hasValidSignature(contentType: String, bytes: ByteArray): Boolean = when (contentType) {
        "image/jpeg" -> bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()
        "image/png" -> bytes.size >= 8 &&
            bytes.copyOfRange(0, 8).contentEquals(
                byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
            )
        "image/webp" -> bytes.size >= 12 &&
            bytes.copyOfRange(0, 4).contentEquals(byteArrayOf(0x52, 0x49, 0x46, 0x46)) &&
            bytes.copyOfRange(8, 12).contentEquals(byteArrayOf(0x57, 0x45, 0x42, 0x50))
        else -> false
    }
}
