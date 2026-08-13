package com.attendance.tracker.feature.ocr.repository

import android.content.Context
import android.net.Uri
import com.attendance.tracker.feature.ocr.model.OcrTimetableRow
import com.attendance.tracker.feature.ocr.model.OcrAttendanceRow
import com.attendance.tracker.feature.ocr.model.OcrField
import com.attendance.tracker.feature.ocr.scanner.OcrScanner
import com.attendance.tracker.feature.ocr.parser.OcrParser
import com.attendance.tracker.feature.ocr.processing.ImageProcessor
import com.attendance.tracker.feature.ocr.processing.PreprocessedImage
import com.attendance.tracker.feature.ocr.processing.QualityCheckResult
import com.attendance.tracker.feature.ocr.detection.TableDetector
import com.attendance.tracker.feature.ocr.detection.GridMasks
import com.attendance.tracker.feature.ocr.structure.OpenCVGridDetector
import com.attendance.tracker.feature.ocr.structure.TableGridModel
import com.attendance.tracker.feature.ocr.extraction.CellExtractor
import com.attendance.tracker.feature.ocr.recognition.OcrRecognizer
import com.attendance.tracker.feature.ocr.recognition.RecognizedCell
import com.attendance.tracker.feature.ocr.parser.SemanticParser
import com.attendance.tracker.feature.ocr.diagnostics.OcrInstrumentation
import com.attendance.tracker.core.common.DispatcherProvider
import com.attendance.tracker.core.opencv.OpenCVInitializer
import org.opencv.core.Rect
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * Coordinates image preprocessing, table grid segmentation, text recognition, and semantic parsing
 * to extract structured data from screenshots.
 */
class OcrRepository @Inject constructor(
    private val scanner: OcrScanner,
    private val parser: OcrParser,
    private val imageProcessor: ImageProcessor,
    private val tableDetector: TableDetector,
    private val gridDetector: OpenCVGridDetector,
    private val cellExtractor: CellExtractor,
    private val ocrRecognizer: OcrRecognizer,
    private val semanticParser: SemanticParser,
    private val dispatcherProvider: DispatcherProvider,
    private val openCVInitializer: OpenCVInitializer
) {
    /**
     * Scans an image, runs quality check, segments table cells, performs OCR cell-by-cell,
     * and compiles structural timetable class rows.
     */
    suspend fun importTimetable(context: Context, uri: Uri): Result<List<OcrTimetableRow>> =
        withContext(dispatcherProvider.default) {
            val pipelineStart = System.nanoTime()
            OcrInstrumentation.i(OcrInstrumentation.TAG_PIPELINE, "PIPELINE ENTRY uri=$uri")

            if (!openCVInitializer.ensureLoaded()) {
                OcrInstrumentation.i(OcrInstrumentation.TAG_PIPELINE, "PIPELINE EXIT OpenCV unavailable")
                return@withContext Result.failure(Exception("OpenCV is not available on this device."))
            }
            val matResult = imageProcessor.loadMatFromUri(context, uri)
            if (matResult.isFailure) {
                OcrInstrumentation.i(
                    OcrInstrumentation.TAG_PIPELINE,
                    "PIPELINE EXIT loadMat failure=${matResult.exceptionOrNull()?.message}"
                )
                return@withContext Result.failure(
                    matResult.exceptionOrNull() ?: Exception("Failed to load image from URI")
                )
            }
            val originalMat = matResult.getOrThrow()
            val originalDims = try {
                "${originalMat.cols()}x${originalMat.rows()}"
            } catch (t: Throwable) {
                "unknown"
            }
            var preprocessed: PreprocessedImage? = null
            var gridMasks: GridMasks? = null
            try {
                // 1. Image Quality Check
                val quality = imageProcessor.checkQuality(originalMat)
                if (quality is QualityCheckResult.Fail) {
                    OcrInstrumentation.i(OcrInstrumentation.TAG_PIPELINE, "PIPELINE EXIT quality fail: ${quality.reason}")
                    return@withContext Result.failure(Exception(quality.reason))
                }

                // 2. Image Preprocessing
                val prep = imageProcessor.preprocess(originalMat)
                preprocessed = prep
                
                // 3. Grid Lines Extraction
                val masks = tableDetector.extractGridMasks(prep.threshMat)
                gridMasks = masks
                
                // 4. Table Detection
                val tables = tableDetector.detectTables(prep.threshMat, masks)
                if (tables.isEmpty()) {
                    OcrInstrumentation.i(OcrInstrumentation.TAG_PIPELINE, "PIPELINE EXIT no tables detected")
                    return@withContext Result.failure(
                        Exception("Could not detect any table grids. Please ensure the timetable borders are clearly visible.")
                    )
                }

                // The first detected table (highest y coordinate) is the main timetable
                val timetableRect = tables.first()

                // 5. Grid Structure detection
                val gridModel = gridDetector.detectStructure(
                    timetableRect,
                    masks.horizontal,
                    masks.vertical,
                    masks.combined
                )

                // 6. Cell Extraction
                val extractedCells = cellExtractor.extractCells(prep.originalMat, gridModel.cells)

                // 7. Cell-by-cell Text Recognition
                val recognizedCells = ocrRecognizer.recognizeCells(extractedCells)

                // 8. Semantic parsing into classes
                val parsedResult = semanticParser.parse(recognizedCells)

                val mappedRows = parsedResult.subjects.map { subjectClass ->
                    OcrTimetableRow(
                        id = UUID.randomUUID().toString(),
                        subjectName = OcrField(subjectClass.subject + (if (subjectClass.lab) " LAB" else ""), 0.9f),
                        dayOfWeek = OcrField(subjectClass.day, 0.95f),
                        startTime = OcrField(subjectClass.start, 0.95f),
                        endTime = OcrField(subjectClass.end, 0.95f),
                        faculty = OcrField(subjectClass.faculty, 0.9f),
                        room = OcrField(null, 1.0f)
                    )
                }

                emitPipelineSummary(
                    uri = uri,
                    originalDims = originalDims,
                    tables = tables,
                    gridModel = gridModel,
                    recognizedCells = recognizedCells,
                    parsedCount = parsedResult.subjects.size,
                    pipelineElapsedMs = OcrInstrumentation.elapsedMs(pipelineStart)
                )
                if (parsedResult.subjects.isEmpty()) {
                    emitFailureSnapshot(uri, originalDims, tables, gridModel, recognizedCells)
                }
                OcrInstrumentation.i(
                    OcrInstrumentation.TAG_PIPELINE,
                    "PIPELINE EXIT success subjects=${parsedResult.subjects.size} " +
                        "elapsed=${OcrInstrumentation.elapsedMs(pipelineStart)}ms"
                )

                Result.success(mappedRows)
            } catch (e: Exception) {
                OcrInstrumentation.e(OcrInstrumentation.TAG_PIPELINE, "PIPELINE EXIT exception", e)
                Result.failure(e)
            } finally {
                originalMat.release()
                gridMasks?.horizontal?.release()
                gridMasks?.vertical?.release()
                gridMasks?.combined?.release()
                preprocessed?.originalMat?.release()
                preprocessed?.grayMat?.release()
                preprocessed?.threshMat?.release()
            }
        }

    /**
     * Emits one compact pipeline summary line for every OCR session so a single
     * surviving log entry captures the whole image -> rows chain even if earlier
     * per-stage entries are evicted from the bounded diagnostics buffer.
     */
    private fun emitPipelineSummary(
        uri: Uri,
        originalDims: String,
        tables: List<Rect>,
        gridModel: TableGridModel,
        recognizedCells: List<RecognizedCell>,
        parsedCount: Int,
        pipelineElapsedMs: Long
    ) {
        val withText = recognizedCells.count { it.text.isNotBlank() }
        OcrInstrumentation.i(
            OcrInstrumentation.TAG_SUMMARY,
            "SUMMARY uri=$uri image=$originalDims tables=${tables.size} " +
                "grid=${gridModel.rows}x${gridModel.cols} cells=${gridModel.cells.size} " +
                "ocrCells=${recognizedCells.size} ocrWithText=$withText parsedSubjects=$parsedCount " +
                "elapsed=${pipelineElapsedMs}ms"
        )
    }

    /**
     * Emits a failure snapshot when the parser returns an empty list. Together with
     * the SemanticParser debug lines (header row, day column, per-cell skip reasons)
     * this pinpoints which upstream stage produced no usable structures.
     */
    private fun emitFailureSnapshot(
        uri: Uri,
        originalDims: String,
        tables: List<Rect>,
        gridModel: TableGridModel,
        recognizedCells: List<RecognizedCell>
    ) {
        val withText = recognizedCells.filter { it.text.isNotBlank() }
        val withoutText = recognizedCells.size - withText.size
        val emptyRate = if (recognizedCells.isEmpty()) 0f else withoutText.toFloat() / recognizedCells.size
        val sampleTexts = withText.take(10).joinToString(" | ") { it.text.replace(Regex("\\s+"), " ").take(30) }
        OcrInstrumentation.i(
            OcrInstrumentation.TAG_FAILURE,
            "FAILURE uri=$uri image=$originalDims tables=${tables.size} grid=${gridModel.rows}x${gridModel.cols} " +
                "cells=${recognizedCells.size} withText=${withText.size} withoutText=$withoutText emptyRate=$emptyRate " +
                "subjects=0 sampleTexts='$sampleTexts' " +
                "-> see SemanticParser debug lines for header/day/time rejection reasons"
        )
    }

    /**
     * Scans an image and extracts attendance records.
     */
    suspend fun importAttendance(context: Context, uri: Uri): Result<List<OcrAttendanceRow>> {
        val scanResult = scanner.scanImage(context, uri)
        return scanResult.map { parser.parseAttendance(it) }
    }
}
