package com.attendance.tracker.feature.ocr.recognition

import org.opencv.core.Rect
import com.attendance.tracker.feature.ocr.extraction.ExtractedCell
import com.attendance.tracker.feature.ocr.scanner.OcrScanner
import com.attendance.tracker.feature.ocr.diagnostics.OcrInstrumentation
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
        val ocrStart = System.nanoTime()
        val recognized = ArrayList<RecognizedCell>()

        for (cell in extractedCells) {
            val cellStart = System.nanoTime()
            val result = ocrScanner.scanBitmap(cell.bitmap)
            // The cell bitmap has been consumed by ML Kit and is not referenced
            // anywhere downstream, so it is recycled immediately.
            cell.bitmap.recycle()
            val textObj = result.getOrNull()
            val text = textObj?.text?.trim().orEmpty()
            val failure = result.exceptionOrNull()
            val failureMessage = failure?.message?.take(120)

            if (failure != null) {
                OcrInstrumentation.e(
                    OcrInstrumentation.TAG_CELL_OCR,
                    "CELL_OCR FAILURE row=${cell.startRow} col=${cell.startCol} message=$failureMessage",
                    failure
                )
            }

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

            val logText = text.replace(Regex("\\s+"), " ").take(80)
            OcrInstrumentation.i(
                OcrInstrumentation.TAG_CELL_OCR,
                "CELL_OCR row=${cell.startRow} col=${cell.startCol} " +
                    "rect=(${cell.rect.x},${cell.rect.y},${cell.rect.width},${cell.rect.height}) " +
                    "text='$logText' len=${text.length} empty=${text.isEmpty()} confidence=$confidence " +
                    "failure=${failureMessage ?: "none"} " +
                    "elapsed=${OcrInstrumentation.elapsedMs(cellStart)}ms"
            )

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

        val withText = recognized.count { it.text.isNotBlank() }
        OcrInstrumentation.i(
            OcrInstrumentation.TAG_CELL_OCR,
            "CELL_OCR total=${recognized.size} withText=$withText empty=${recognized.size - withText} " +
                "elapsed=${OcrInstrumentation.elapsedMs(ocrStart)}ms"
        )

        return recognized
    }
}
