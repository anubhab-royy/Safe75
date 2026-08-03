package com.attendance.tracker.feature.ocr.extraction

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Rect
import com.attendance.tracker.feature.ocr.structure.TableGridCell
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Container holding a cropped cell bitmap along with its grid coordinate information.
 */
data class ExtractedCell(
    val id: String,
    val startRow: Int,
    val startCol: Int,
    val rowSpan: Int,
    val colSpan: Int,
    val rect: Rect,
    val bitmap: Bitmap
)

/**
 * Extracts and crops individual cell images from the source image.
 * Uses OpenCV Sub-Mats natively before converting to Android Bitmaps for memory efficiency.
 */
@Singleton
class CellExtractor @Inject constructor() {

    /**
     * Crops cell regions from the original Mat and converts them to Bitmaps.
     *
     * @param originalMat Original color or grayscale source image.
     * @param cells List of structural cells detected in the grid.
     * @return List of [ExtractedCell] containing cropped bitmaps.
     */
    fun extractCells(originalMat: Mat, cells: List<TableGridCell>): List<ExtractedCell> {
        val extracted = ArrayList<ExtractedCell>()
        val colsLimit = originalMat.cols()
        val rowsLimit = originalMat.rows()

        for (cell in cells) {
            val r = cell.rect
            // Ensure bounding boxes lie strictly within image boundaries
            val x = r.x.coerceIn(0, colsLimit - 1)
            val y = r.y.coerceIn(0, rowsLimit - 1)
            val width = r.width.coerceAtMost(colsLimit - x)
            val height = r.height.coerceAtMost(rowsLimit - y)

            if (width <= 0 || height <= 0) continue

            val safeRect = Rect(x, y, width, height)
            val cellMat = Mat(originalMat, safeRect)
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(cellMat, bitmap)
            cellMat.release()

            extracted.add(
                ExtractedCell(
                    id = cell.id,
                    startRow = cell.startRow,
                    startCol = cell.startCol,
                    rowSpan = cell.rowSpan,
                    colSpan = cell.colSpan,
                    rect = r,
                    bitmap = bitmap
                )
            )
        }
        return extracted
    }
}
