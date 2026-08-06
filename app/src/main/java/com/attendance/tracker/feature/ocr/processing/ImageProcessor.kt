package com.attendance.tracker.feature.ocr.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.attendance.tracker.BuildConfig
import com.attendance.tracker.core.logger.Logger
import com.attendance.tracker.feature.ocr.diagnostics.OcrInstrumentation
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Result of the quality check.
 */
sealed class QualityCheckResult {
    object Pass : QualityCheckResult()
    data class Fail(val reason: String) : QualityCheckResult()
}

/**
 * Preprocessed image container holding the original, grayscale, and thresholded Mats.
 */
data class PreprocessedImage(
    val originalMat: Mat,
    val grayMat: Mat,
    val threshMat: Mat
)

/**
 * Handles image loading, resolution, blur and contrast quality checks,
 * and OpenCV-based image preprocessing (grayscale, bilateral filter denoising, deskew, and thresholding).
 */
@Singleton
class ImageProcessor @Inject constructor() {

    companion object {
        /**
         * Longest-edge cap for OCR input. Modern phone screenshots (12-50 MP)
         * far exceed what OCR grid detection and ML Kit text recognition can use.
         * Decoding with a power-of-two sample size lands the longest edge in
         * (MAX_LONGEST_EDGE / 2, MAX_LONGEST_EDGE] while never upscaling.
         */
        const val MAX_LONGEST_EDGE = 2048

        /**
         * Floor mirroring the quality-check resolution gate ([checkQuality] rejects
         * images shorter than 400px on either edge). Downsampling must not push a
         * sane image below this and newly fail the low-resolution gate.
         */
        const val MIN_SHORTEST_EDGE = 400
    }

    /**
     * Chooses a power-of-two decode sample size so the resulting longest edge is
     * at most [maxLongestEdge] (and at least half of it). If that would shrink the
     * shortest edge below [minShortestEdge], a smaller sample size is used instead,
     * keeping realistic timetable screenshots above the quality-check resolution floor.
     */
    internal fun computeSampleSize(
        width: Int,
        height: Int,
        maxLongestEdge: Int = MAX_LONGEST_EDGE,
        minShortestEdge: Int = MIN_SHORTEST_EDGE
    ): Int {
        var sample = 1
        var longest = maxOf(width, height)
        while (longest / sample > maxLongestEdge) {
            sample *= 2
        }
        val shortestScaled = minOf(width, height) / sample
        if (shortestScaled < minShortestEdge && sample > 1) {
            sample /= 2
        }
        return sample
    }

    /**
     * Loads a Bitmap from a content URI and converts it to an OpenCV Mat,
     * decoding at a downsampled resolution when the source exceeds the OCR
     * working resolution. The intermediate Bitmap is recycled immediately
     * after its pixels are copied into the Mat.
     */
    fun loadMatFromUri(context: Context, uri: Uri): Result<Mat> {
        return try {
            val decodeStart = System.nanoTime()
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, bounds)
            inputStream?.close()

            val width = bounds.outWidth
            val height = bounds.outHeight
            if (width <= 0 || height <= 0) {
                OcrInstrumentation.i(OcrInstrumentation.TAG_DECODE, "DECODE FAIL uri=$uri bounds=${width}x${height}")
                return Result.failure(Exception("Failed to decode image bounds from URI"))
            }

            val sampleSize = computeSampleSize(width, height)
            OcrInstrumentation.i(OcrInstrumentation.TAG_DECODE, "DECODE uri=$uri source=${width}x${height} sampleSize=$sampleSize")

            val decodeStream: InputStream? = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = BitmapFactory.decodeStream(decodeStream, null, options)
            decodeStream?.close()
            if (bitmap == null) {
                OcrInstrumentation.i(OcrInstrumentation.TAG_DECODE, "DECODE FAIL bitmap null uri=$uri")
                return Result.failure(Exception("Failed to decode bitmap from URI"))
            }
            OcrInstrumentation.i(
                OcrInstrumentation.TAG_DECODE,
                "DECODE bitmap=${bitmap.width}x${bitmap.height} config=${bitmap.config} bytes=${bitmap.allocationByteCount} " +
                    "rotation=NOT_APPLIED elapsed=${OcrInstrumentation.elapsedMs(decodeStart)}ms"
            )
            Logger.i("ImageProcessor", "decoded ${bitmap.width}x${bitmap.height} from source ${width}x${height}")
            if (BuildConfig.DEBUG) {
                runCatching {
                    val f = java.io.File(context.cacheDir, "ocr_input.png")
                    context.contentResolver.openOutputStream(android.net.Uri.fromFile(f))?.use { os ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                    }
                }
            }
            val mat = Mat()
            Utils.bitmapToMat(bitmap, mat)
            bitmap.recycle()
            Result.success(mat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Performs a quality check detecting low resolution, blurriness, and low contrast.
     */
    fun checkQuality(mat: Mat): QualityCheckResult {
        val width = mat.cols()
        val height = mat.rows()

        // 1. Resolution Check
        if (width < 400 || height < 400) {
            return QualityCheckResult.Fail("Image resolution is too low ($width x $height). Please capture or crop a higher resolution image.")
        }

        // Convert to grayscale for blur and contrast checks
        val gray = Mat()
        if (mat.channels() > 1) {
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
        } else {
            mat.copyTo(gray)
        }

        // 2. Contrast Check
        val meanG = MatOfDouble()
        val stddevG = MatOfDouble()
        Core.meanStdDev(gray, meanG, stddevG)
        val contrast = stddevG.toArray()[0]
        OcrInstrumentation.i(OcrInstrumentation.TAG_QUALITY, "QUALITY contrast=$contrast")
        if (contrast < 15.0) {
            gray.release()
            OcrInstrumentation.i(OcrInstrumentation.TAG_QUALITY, "QUALITY FAIL low contrast ($contrast)")
            return QualityCheckResult.Fail("Image contrast is too low ($contrast). Please ensure the timetable is clear and well-lit.")
        }

        // 3. Blur Check (Laplacian Variance)
        val laplacian = Mat()
        Imgproc.Laplacian(gray, laplacian, CvType.CV_64F)
        val meanL = MatOfDouble()
        val stddevL = MatOfDouble()
        Core.meanStdDev(laplacian, meanL, stddevL)
        val variance = stddevL.toArray()[0] * stddevL.toArray()[0]
        
        laplacian.release()
        gray.release()

        // Threshold for blurriness: lower values mean more blurry
        if (variance < 60.0) {
            OcrInstrumentation.i(OcrInstrumentation.TAG_QUALITY, "QUALITY FAIL blur variance ($variance)")
            return QualityCheckResult.Fail("Image is too blurry ($variance). Please hold the device steady and retake the photo.")
        }

        OcrInstrumentation.i(OcrInstrumentation.TAG_QUALITY, "QUALITY PASS resolution=${width}x${height} contrast=$contrast blurVariance=$variance")
        return QualityCheckResult.Pass
    }

    /**
     * Preprocesses the image: Grayscale, bilateral filter denoise, deskew, and thresholding.
     */
    fun preprocess(mat: Mat): PreprocessedImage {
        val prepStart = System.nanoTime()
        OcrInstrumentation.i(
            OcrInstrumentation.TAG_PREPROCESS,
            "PREPROCESS input=${mat.cols()}x${mat.rows()} channels=${mat.channels()} type=${mat.type()}"
        )
        val gray = Mat()
        if (mat.channels() > 1) {
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
        } else {
            mat.copyTo(gray)
        }

        // 1. Denoise with bilateral filter to preserve sharp edges of grid lines
        val denoised = Mat()
        Imgproc.bilateralFilter(gray, denoised, 9, 75.0, 75.0)
        gray.release()

        // 2. Auto Deskew. When no rotation is applied, [deskew] returns the same
        //    Mat instance (no copy), so it must not be released separately.
        val deskewed = deskew(denoised)
        if (deskewed !== denoised) {
            denoised.release()
        }

        // 3. Adaptive Thresholding to binarize the image (clean text and grids)
        val thresh = Mat()
        Imgproc.adaptiveThreshold(
            deskewed,
            thresh,
            255.0,
            Imgproc.ADAPTIVE_THRESH_MEAN_C,
            Imgproc.THRESH_BINARY_INV,
            15,
            8.0
        )

        // Create a copy of originalMat rotated/warped if deskewed was modified
        // In this deskew implementation we align sizes
        val processedOriginal = Mat()
        if (mat.size() != deskewed.size()) {
            // Apply same deskew rotation to originalMat
            Imgproc.resize(mat, processedOriginal, deskewed.size())
        } else {
            mat.copyTo(processedOriginal)
        }

        OcrInstrumentation.i(
            OcrInstrumentation.TAG_PREPROCESS,
            "PREPROCESS output original=${processedOriginal.cols()}x${processedOriginal.rows()} " +
                "gray=${deskewed.cols()}x${deskewed.rows()} thresh=${thresh.cols()}x${thresh.rows()} " +
                "elapsed=${OcrInstrumentation.elapsedMs(prepStart)}ms"
        )

        return PreprocessedImage(processedOriginal, deskewed, thresh)
    }

    /**
     * Detects skew angle and rotates the image to straighten it.
     * Returns the input Mat itself when no rotation is applied (no copy is made).
     */
    private fun deskew(gray: Mat): Mat {
        val binary = Mat()
        Imgproc.threshold(gray, binary, 0.0, 255.0, Imgproc.THRESH_BINARY_INV or Imgproc.THRESH_OTSU)

        // Find non-zero points (text pixels) to compute orientation
        val points = MatOfPoint()
        Core.findNonZero(binary, points)

        if (points.empty()) {
            binary.release()
            points.release()
            return gray
        }

        // Convert int points to float without materializing a Java Point array
        // (native conversion avoids a large Java-heap allocation for dense text).
        val points2f = MatOfPoint2f()
        points.convertTo(points2f, CvType.CV_32FC2)
        val rect = Imgproc.minAreaRect(points2f)

        var angle = rect.angle
        if (angle < -45) {
            angle += 90.0
        }

        binary.release()
        points.release()
        points2f.release()

        // Only rotate if the angle is significant and within reason
        if (abs(angle) > 0.5 && abs(angle) < 15.0) {
            OcrInstrumentation.i(OcrInstrumentation.TAG_PREPROCESS, "PREPROCESS deskew APPLIED angle=$angle")
            val rotMat = Imgproc.getRotationMatrix2D(rect.center, angle, 1.0)
            val deskewed = Mat()
            Imgproc.warpAffine(
                gray,
                deskewed,
                rotMat,
                gray.size(),
                Imgproc.INTER_CUBIC,
                Core.BORDER_REPLICATE
            )
            rotMat.release()
            return deskewed
        }

        OcrInstrumentation.i(OcrInstrumentation.TAG_PREPROCESS, "PREPROCESS deskew SKIPPED angle=$angle")
        return gray
    }
}
