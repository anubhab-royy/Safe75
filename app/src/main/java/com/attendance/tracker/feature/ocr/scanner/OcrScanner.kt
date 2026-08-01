package com.attendance.tracker.feature.ocr.scanner

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Controller using Google ML Kit Text Recognition to extract text elements and blocks.
 */
class OcrScanner @Inject constructor() {

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    /**
     * Extracts text elements from an image URI.
     */
    suspend fun scanImage(context: Context, uri: Uri): Result<Text> {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            val result = recognizer.process(image).await()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extracts text elements from a Bitmap source.
     */
    suspend fun scanBitmap(bitmap: Bitmap, rotation: Int = 0): Result<Text> {
        return try {
            val image = InputImage.fromBitmap(bitmap, rotation)
            val result = recognizer.process(image).await()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Custom suspend wrapper to await Task results off-main-thread.
     */
    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }
        addOnFailureListener { exception ->
            continuation.resumeWithException(exception)
        }
    }
}
