package com.attendance.tracker.feature.ocr.recognition

import org.opencv.core.Rect
import com.attendance.tracker.feature.ocr.extraction.ExtractedCell
import com.attendance.tracker.feature.ocr.scanner.OcrScanner
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data structure representing a cell containing the recognized OCR text and its confidence score.
 */
data class RecognizedCell(
    val id: String,
    val startRow: Int,
    val startCol: Int,
    val rowSpan: Int,
    val colSpan: Int,
    val rect: Rect,
    val text: String,
    val confidence: Float
)

/**
 * Coordinates cell-by-cell text recognition using Google ML Kit.
 */
@Singleton
class OcrRecognizer @Inject constructor(
    private val ocrScanner: OcrScanner
) {

    /**
     * Runs text recognition on a list of cropped cell images.
     */
    suspend fun recognizeCells(extractedCells: List<ExtractedCell>): List<RecognizedCell> {
        val recognized = ArrayList<RecognizedCell>()

        for (cell in extractedCells) {
            val result = ocrScanner.scanBitmap(cell.bitmap)
            // The cell bitmap has been consumed by ML Kit and is not referenced
            // anywhere downstream, so it is recycled immediately.
            cell.bitmap.recycle()
            val textObj = result.getOrNull()
            val text = textObj?.text?.trim().orEmpty()

            var sumConfidence = 0.0f
            var count = 0
            textObj?.textBlocks?.forEach { block ->
                block.lines.forEach { line ->
                    line.elements.forEach { element ->
                        sumConfidence += element.confidence
                        count++
                    }
                }
            }
            val confidence = if (count > 0) sumConfidence / count else 0.85f

            recognized.add(
                RecognizedCell(
                    id = cell.id,
                    startRow = cell.startRow,
                    startCol = cell.startCol,
                    rowSpan = cell.rowSpan,
                    colSpan = cell.colSpan,
                    rect = cell.rect,
                    text = text,
                    confidence = confidence
                )
            )
        }

        return recognized
    }
}
