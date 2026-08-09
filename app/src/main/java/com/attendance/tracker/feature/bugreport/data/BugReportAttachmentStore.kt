package com.attendance.tracker.feature.bugreport.data

import android.content.Context
import com.attendance.tracker.feature.bugreport.model.ScreenshotConstraints
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import kotlinx.coroutines.withContext
import com.attendance.tracker.core.common.DispatcherProvider
import javax.inject.Inject
import javax.inject.Singleton

/** Stores the exact selected image in app-private storage until upload succeeds. */
@Singleton
class BugReportAttachmentStore @Inject constructor(
    @ApplicationContext context: Context,
    private val dispatcherProvider: DispatcherProvider
) {
    private val root = File(context.filesDir, DIRECTORY_NAME)

    suspend fun write(reportId: String, bytes: ByteArray): String = withContext(dispatcherProvider.io) {
        require(bytes.size <= ScreenshotConstraints.MAX_BYTES) { "Screenshot exceeds maximum size" }
        if (!root.exists() && !root.mkdirs()) {
            throw IOException("Unable to create private attachment directory")
        }
        val target = File(root, "$reportId.image")
        val temporary = File(root, "$reportId.image.tmp")
        temporary.outputStream().use { it.write(bytes) }
        if (!temporary.renameTo(target)) {
            temporary.delete()
            throw IOException("Unable to finalize screenshot attachment")
        }
        target.canonicalPath
    }

    suspend fun read(path: String): ByteArray? = withContext(dispatcherProvider.io) {
        val file = File(path)
        if (!file.isFile || file.length() > ScreenshotConstraints.MAX_BYTES) {
            return@withContext null
        }
        runCatching { file.readBytes() }.getOrNull()
    }

    suspend fun delete(path: String?) = withContext(dispatcherProvider.io) {
        if (!path.isNullOrBlank()) {
            File(path).delete()
        }
    }

    suspend fun cleanupTemporaryFiles() = withContext(dispatcherProvider.io) {
        root.listFiles { file -> file.name.endsWith(".tmp") }?.forEach { it.delete() }
    }

    private companion object {
        const val DIRECTORY_NAME = "bug-report-attachments"
    }
}
