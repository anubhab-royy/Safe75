package com.attendance.tracker.feature.ocr.detection

import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import com.attendance.tracker.feature.ocr.diagnostics.OcrInstrumentation
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result container for the extracted horizontal and vertical grid masks of a table.
 */
data class GridMasks(
    val horizontal: Mat,
    val vertical: Mat,
    val combined: Mat
)

/**
 * Detects distinct, independent tables in an image (such as the main timetable and the faculty mapping).
 * Does not assume fixed coordinates.
 */
@Singleton
class TableDetector @Inject constructor() {

    /**
     * Extracts horizontal and vertical lines to build grid masks from a binarized threshold image.
     */
    fun extractGridMasks(threshMat: Mat): GridMasks {
        val horizontal = threshMat.clone()
        val scaleH = 35 // Kernel scale factor for horizontal lines
        val horizontalSize = maxOf(1, horizontal.cols() / scaleH)
        val horizontalStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(horizontalSize.toDouble(), 1.0))
        Imgproc.erode(horizontal, horizontal, horizontalStructure)
        Imgproc.dilate(horizontal, horizontal, horizontalStructure)
        horizontalStructure.release()

        val vertical = threshMat.clone()
        val scaleV = 35 // Kernel scale factor for vertical lines
        val verticalSize = maxOf(1, vertical.rows() / scaleV)
        val verticalStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(1.0, verticalSize.toDouble()))
        Imgproc.erode(vertical, vertical, verticalStructure)
        Imgproc.dilate(vertical, vertical, verticalStructure)
        verticalStructure.release()

        val combined = Mat()
        Core.bitwise_or(horizontal, vertical, combined)

        return GridMasks(horizontal, vertical, combined)
    }

    /**
     * Locates independent tables sorted vertically (top-to-bottom).
     *
     * @param threshMat Binarized threshold image.
     * @param gridMasks Extracted horizontal and vertical line masks.
     * @return List of [Rect] representing table boundaries.
     */
    fun detectTables(threshMat: Mat, gridMasks: GridMasks): List<Rect> {
        val tableStart = System.nanoTime()
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        
        // Find external outermost contours of grid lines
        Imgproc.findContours(
            gridMasks.combined,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        val widthLimit = threshMat.cols() * 0.25
        val heightLimit = threshMat.rows() * 0.08

        val tables = contours.map { Imgproc.boundingRect(it) }
            .filter { it.width >= widthLimit && it.height >= heightLimit }
            .sortedBy { it.y } // Sort top-to-bottom

        hierarchy.release()
        contours.forEach { it.release() }

        OcrInstrumentation.i(
            OcrInstrumentation.TAG_TABLE,
            "TABLE tables=${tables.size} image=${threshMat.cols()}x${threshMat.rows()} " +
                "rects=${tables.joinToString(prefix = "[", postfix = "]") { "(${it.x},${it.y},${it.width},${it.height})" }} " +
                "elapsed=${OcrInstrumentation.elapsedMs(tableStart)}ms"
        )

        return tables
    }
}
