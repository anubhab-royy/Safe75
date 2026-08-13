package com.attendance.tracker.backend.util

import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.max

/**
 * Handles image validation, downscaling, metadata stripping, and format transformation.
 */
object ImageProcessor {

    /**
     * Checks magic bytes of a file block to determine the image content type.
     * Supported formats: JPEG, PNG, WEBP.
     */
    fun detectFormat(bytes: ByteArray): String? {
        if (bytes.size < 4) return null

        // JPEG Magic: FF D8 FF
        if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
            return "image/jpeg"
        }

        // PNG Magic: 89 50 4E 47 0D 0A 1A 0A
        if (bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() &&
            bytes[4] == 0x0D.toByte() && bytes[5] == 0x0A.toByte() &&
            bytes[6] == 0x1A.toByte() && bytes[7] == 0x0A.toByte()
        ) {
            return "image/png"
        }

        // WEBP Magic: RIFF + WEBP
        if (bytes.size >= 12 &&
            bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        ) {
            return "image/webp"
        }

        return null
    }

    /**
     * Reads image content, downscales it if the longest edge exceeds 1600px while maintaining
     * aspect ratio, strips all metadata (EXIF/segments), and returns a raw JPEG byte array.
     */
    fun processImage(bytes: ByteArray): ByteArray {
        val format = detectFormat(bytes) ?: throw IllegalArgumentException("Unsupported or unrecognized image format")

        val inputStream = ByteArrayInputStream(bytes)
        val image = try {
            ImageIO.read(inputStream)
        } catch (e: Exception) {
            null
        } ?: throw IllegalArgumentException("Corrupted or unreadable image file")

        val width = image.width
        val height = image.height
        val maxEdge = max(width, height)

        val processedImage = if (maxEdge > 1600) {
            val scaleFactor = 1600.0 / maxEdge
            val targetWidth = (width * scaleFactor).toInt()
            val targetHeight = (height * scaleFactor).toInt()

            val scaledImage = image.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH)
            val outputImage = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)

            val g2d = outputImage.createGraphics()
            g2d.drawImage(scaledImage, 0, 0, null)
            g2d.dispose()
            outputImage
        } else {
            // Convert to TYPE_INT_RGB to remove transparency/alpha and drop meta tags automatically
            val outputImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            val g2d = outputImage.createGraphics()
            g2d.drawImage(image, 0, 0, null)
            g2d.dispose()
            outputImage
        }

        val outStream = ByteArrayOutputStream()
        val success = ImageIO.write(processedImage, "jpg", outStream)
        if (!success) {
            throw RuntimeException("Failed to write compressed image data to buffer")
        }

        return outStream.toByteArray()
    }
}
