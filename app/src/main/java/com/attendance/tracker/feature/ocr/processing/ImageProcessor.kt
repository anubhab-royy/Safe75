package com.attendance.tracker.feature.ocr.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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

    /**
     * Loads a Bitmap from a content URI and converts it to an OpenCV Mat.
     */
    fun loadMatFromUri(context: Context, uri: Uri): Result<Mat> {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap == null) {
                return Result.failure(Exception("Failed to decode bitmap from URI"))
            }
            val mat = Mat()
            Utils.bitmapToMat(bitmap, mat)
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
        if (contrast < 15.0) {
            gray.release()
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
            return QualityCheckResult.Fail("Image is too blurry ($variance). Please hold the device steady and retake the photo.")
        }

        return QualityCheckResult.Pass
    }

    /**
     * Preprocesses the image: Grayscale, bilateral filter denoise, deskew, and thresholding.
     */
    fun preprocess(mat: Mat): PreprocessedImage {
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

        // 2. Auto Deskew
        val deskewed = deskew(denoised)
        denoised.release()

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

        return PreprocessedImage(processedOriginal, deskewed, thresh)
    }

    /**
     * Detects skew angle and rotates the image to straighten it.
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
            return gray.clone()
        }

        val points2f = MatOfPoint2f(*points.toArray())
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

        return gray.clone()
    }
}
