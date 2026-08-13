package com.attendance.tracker.feature.bugreport.data

import android.content.Context
import android.net.Uri
import com.attendance.tracker.feature.bugreport.model.ScreenshotAttachment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import android.webkit.MimeTypeMap
import javax.inject.Inject

interface ScreenshotContentReader {
    suspend fun read(uri: String): Result<ScreenshotAttachment>
}

class AndroidScreenshotContentReader @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ScreenshotContentReader {

    override suspend fun read(uri: String): Result<ScreenshotAttachment> {
        return runCatching {
            val parsedUri = Uri.parse(uri)
            val contentType = context.contentResolver.getType(parsedUri)
                ?: parsedUri.lastPathSegment
                    ?.substringAfterLast('.', "")
                    ?.let(MimeTypeMap.getSingleton()::getMimeTypeFromExtension)
                ?: throw IOException("Image type could not be determined")
            val bytes = context.contentResolver.openInputStream(parsedUri)?.use { input ->
                readAtMost(input, MAX_READ_BYTES)
            } ?: throw IOException("Image could not be opened")
            ScreenshotAttachment(uri, bytes, contentType.substringBefore(';').lowercase())
        }
    }

    private fun readAtMost(input: java.io.InputStream, maxBytes: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (output.size() < maxBytes) {
            val remaining = maxBytes - output.size()
            val read = input.read(buffer, 0, minOf(buffer.size, remaining))
            if (read < 0) break
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private companion object {
        const val MAX_READ_BYTES = 2 * 1024 * 1024 + 1
    }
}
