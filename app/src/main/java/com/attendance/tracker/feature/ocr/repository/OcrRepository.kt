package com.attendance.tracker.feature.ocr.repository

import android.content.Context
import android.net.Uri
import com.attendance.tracker.feature.ocr.model.OcrTimetableRow
import com.attendance.tracker.feature.ocr.model.OcrAttendanceRow
import com.attendance.tracker.feature.ocr.model.OcrField
import com.attendance.tracker.feature.ocr.scanner.OcrScanner
import com.attendance.tracker.feature.ocr.parser.OcrParser
import com.attendance.tracker.feature.ocr.processing.ImageProcessor
import com.attendance.tracker.feature.ocr.processing.QualityCheckResult
import com.attendance.tracker.feature.ocr.detection.TableDetector
import com.attendance.tracker.feature.ocr.structure.OpenCVGridDetector
import com.attendance.tracker.feature.ocr.extraction.CellExtractor
import com.attendance.tracker.feature.ocr.recognition.OcrRecognizer
import com.attendance.tracker.feature.ocr.parser.SemanticParser
import kotlinx.coroutines.Dispatchers
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
    private val semanticParser: SemanticParser
) {
    /**
     * Scans an image, runs quality check, segments table cells, performs OCR cell-by-cell,
     * and compiles structural timetable class rows.
     */
    suspend fun importTimetable(context: Context, uri: Uri): Result<List<OcrTimetableRow>> =
        withContext(Dispatchers.Default) {
            val matResult = imageProcessor.loadMatFromUri(context, uri)
            if (matResult.isFailure) {
                return@withContext Result.failure(
                    matResult.exceptionOrNull() ?: Exception("Failed to load image from URI")
                )
            }
            val originalMat = matResult.getOrThrow()
            try {
                // 1. Image Quality Check
                val quality = imageProcessor.checkQuality(originalMat)
                if (quality is QualityCheckResult.Fail) {
                    return@withContext Result.failure(Exception(quality.reason))
                }

                // 2. Image Preprocessing
                val preprocessed = imageProcessor.preprocess(originalMat)
                
                // 3. Grid Lines Extraction
                val gridMasks = tableDetector.extractGridMasks(preprocessed.threshMat)
                
                // 4. Table Detection
                val tables = tableDetector.detectTables(preprocessed.threshMat, gridMasks)
                if (tables.isEmpty()) {
                    gridMasks.horizontal.release()
                    gridMasks.vertical.release()
                    gridMasks.combined.release()
                    preprocessed.originalMat.release()
                    preprocessed.grayMat.release()
                    preprocessed.threshMat.release()
                    return@withContext Result.failure(
                        Exception("Could not detect any table grids. Please ensure the timetable borders are clearly visible.")
                    )
                }

                // The first detected table (highest y coordinate) is the main timetable
                val timetableRect = tables.first()

                // 5. Grid Structure detection
                val gridModel = gridDetector.detectStructure(
                    timetableRect,
                    gridMasks.horizontal,
                    gridMasks.vertical,
                    gridMasks.combined
                )

                // 6. Cell Extraction
                val extractedCells = cellExtractor.extractCells(preprocessed.originalMat, gridModel.cells)

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

                // Clean up Mat memory allocations to prevent native leaks
                gridMasks.horizontal.release()
                gridMasks.vertical.release()
                gridMasks.combined.release()
                preprocessed.originalMat.release()
                preprocessed.grayMat.release()
                preprocessed.threshMat.release()

                Result.success(mappedRows)
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                originalMat.release()
            }
        }

    /**
     * Scans an image and extracts attendance records.
     */
    suspend fun importAttendance(context: Context, uri: Uri): Result<List<OcrAttendanceRow>> {
        val scanResult = scanner.scanImage(context, uri)
        return scanResult.map { parser.parseAttendance(it) }
    }
}
