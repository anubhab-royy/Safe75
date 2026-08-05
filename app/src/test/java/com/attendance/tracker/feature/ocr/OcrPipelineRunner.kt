package com.attendance.tracker.feature.ocr

import org.junit.Test
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import com.attendance.tracker.feature.ocr.processing.ImageProcessor
import com.attendance.tracker.feature.ocr.processing.QualityCheckResult
import com.attendance.tracker.feature.ocr.detection.TableDetector
import com.attendance.tracker.feature.ocr.structure.OpenCVGridDetector
import com.attendance.tracker.feature.ocr.extraction.CellExtractor
import com.attendance.tracker.feature.ocr.recognition.RecognizedCell
import com.attendance.tracker.feature.ocr.parser.SemanticParser
import com.attendance.tracker.feature.ocr.parser.HeaderInterpreter
import com.attendance.tracker.feature.ocr.validation.ValidationEngine
import java.io.File

class OcrPipelineRunner {

    companion object {
        init {
            try {
                nu.pattern.OpenCV.loadShared()
            } catch (e: Throwable) {
                try {
                    org.opencv.osgi.OpenCVNativeLoader().init()
                } catch (e2: Throwable) {
                    System.loadLibrary(Core.NATIVE_LIBRARY_NAME)
                }
            }
        }
    }

    init {
        com.attendance.tracker.core.logger.Logger.setEngine(com.attendance.tracker.core.logger.NoOpLogEngine())
    }

    private val imageProcessor = ImageProcessor()
    private val tableDetector = TableDetector()
    private val gridDetector = OpenCVGridDetector()
    private val validationEngine = ValidationEngine()
    private val headerInterpreter = HeaderInterpreter()
    private val semanticParser = SemanticParser(
        validationEngine,
        headerInterpreter,
        kotlinx.serialization.json.Json { prettyPrint = true }
    )

    @Test
    fun runCompleteOcrPipeline() {
        val brainDir = "C:/Users/anubh/.gemini/antigravity-ide/brain/e8ce8f7a-c023-4850-8ab4-83328089a723"
        val inputImagePath = "$brainDir/media__1785707227737.jpg"
        
        val inputFile = File(inputImagePath)
        if (!inputFile.exists()) {
            println("Input file not found at: $inputImagePath")
            return
        }

        // 1. Load Original Image
        val originalMat = Imgcodecs.imread(inputImagePath)
        if (originalMat.empty()) {
            println("Failed to read image using OpenCV imread.")
            return
        }
        println("Loaded original image: ${originalMat.cols()}x${originalMat.rows()}")

        // Save original image to output artifacts directory
        val outOriginal = "$brainDir/stage1_original.png"
        Imgcodecs.imwrite(outOriginal, originalMat)
        println("Saved Stage 1 (Original) to $outOriginal")

        // 2. Image Quality Check
        val quality = imageProcessor.checkQuality(originalMat)
        println("Stage 2 (Quality Check) result: $quality")
        if (quality is QualityCheckResult.Fail) {
            println("Image failed quality check: ${quality.reason}")
            return
        }

        // 3. Preprocessing (grayscale, filter, deskew, thresh)
        val preprocessed = imageProcessor.preprocess(originalMat)
        val outPreprocessed = "$brainDir/stage2_preprocessed.png"
        Imgcodecs.imwrite(outPreprocessed, preprocessed.threshMat)
        println("Saved Stage 2 (Preprocessed) to $outPreprocessed")

        // 4. Grid Mask extraction
        val gridMasks = tableDetector.extractGridMasks(preprocessed.threshMat)

        // 5. Table Detection
        val tables = tableDetector.detectTables(preprocessed.threshMat, gridMasks)
        println("Detected ${tables.size} tables.")

        // Write boundaries info to text file
        val boundariesInfoFile = File("$brainDir/stage3_boundaries_info.txt")
        boundariesInfoFile.printWriter().use { out ->
            out.println("Image dimensions: ${originalMat.cols()}x${originalMat.rows()}")
            out.println("Total tables detected: ${tables.size}")
            tables.forEachIndexed { idx, rect ->
                out.println("Table $idx: x=${rect.x}, y=${rect.y}, w=${rect.width}, h=${rect.height}")
            }
        }

        val boundsMat = originalMat.clone()
        for ((idx, rect) in tables.withIndex()) {
            Imgproc.rectangle(boundsMat, rect.tl(), rect.br(), Scalar(0.0, 255.0, 0.0), 4)
            Imgproc.putText(
                boundsMat, 
                "Table $idx", 
                Point(rect.x.toDouble(), (rect.y - 10).toDouble()), 
                Imgproc.FONT_HERSHEY_SIMPLEX, 
                1.0, 
                Scalar(0.0, 255.0, 0.0), 
                2
            )
        }
        val outBoundaries = "$brainDir/stage3_boundaries.png"
        Imgcodecs.imwrite(outBoundaries, boundsMat)
        boundsMat.release()
        println("Saved Stage 3 (Boundaries) to $outBoundaries")

        if (tables.isEmpty()) {
            println("No tables detected.")
            return
        }

        // 6. Timetable Structure Detection (First Table is main Timetable)
        val timetableRect = tables.first()
        
        // Let's use reflection to extract the row/col boundaries for visual overlay
        val subHoriz = Mat(gridMasks.horizontal, timetableRect)
        val subVert = Mat(gridMasks.vertical, timetableRect)
        val subGrid = Mat(gridMasks.combined, timetableRect)

        val hContours = ArrayList<org.opencv.core.MatOfPoint>()
        Imgproc.findContours(subHoriz, hContours, Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        val yCoords = ArrayList<Int>()
        yCoords.add(0)
        yCoords.add(timetableRect.height)
        for (c in hContours) {
            val r = Imgproc.boundingRect(c)
            yCoords.add(r.y + r.height / 2)
        }
        
        // Find clusterCoords method via reflection
        val clusterCoordsMethod = OpenCVGridDetector::class.java.getDeclaredMethod(
            "clusterCoords",
            List::class.java,
            Int::class.java
        )
        clusterCoordsMethod.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val rowBoundaries = clusterCoordsMethod.invoke(gridDetector, yCoords, 12) as List<Int>

        val vContours = ArrayList<org.opencv.core.MatOfPoint>()
        Imgproc.findContours(subVert, vContours, Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        val xCoords = ArrayList<Int>()
        xCoords.add(0)
        xCoords.add(timetableRect.width)
        for (c in vContours) {
            val r = Imgproc.boundingRect(c)
            xCoords.add(r.x + r.width / 2)
        }
        @Suppress("UNCHECKED_CAST")
        val colBoundaries = clusterCoordsMethod.invoke(gridDetector, xCoords, 12) as List<Int>

        // Draw Row & Col overlays
        val gridOverlayMat = originalMat.clone()
        // Draw rows (Red horizontal lines)
        for (y in rowBoundaries) {
            Imgproc.line(
                gridOverlayMat, 
                Point(timetableRect.x.toDouble(), (timetableRect.y + y).toDouble()), 
                Point((timetableRect.x + timetableRect.width).toDouble(), (timetableRect.y + y).toDouble()), 
                Scalar(0.0, 0.0, 255.0), 
                2
            )
        }
        // Draw columns (Blue vertical lines)
        for (x in colBoundaries) {
            Imgproc.line(
                gridOverlayMat, 
                Point((timetableRect.x + x).toDouble(), timetableRect.y.toDouble()), 
                Point((timetableRect.x + x).toDouble(), (timetableRect.y + timetableRect.height).toDouble()), 
                Scalar(255.0, 0.0, 0.0), 
                2
            )
        }
        val outGridOverlay = "$brainDir/stage4_grid_overlay.png"
        Imgcodecs.imwrite(outGridOverlay, gridOverlayMat)
        gridOverlayMat.release()
        println("Saved Stage 4 (Grid Overlay) to $outGridOverlay")

        // 7. Structure & Cell Extraction
        val gridModel = gridDetector.detectStructure(
            timetableRect,
            gridMasks.horizontal,
            gridMasks.vertical,
            gridMasks.combined
        )
        println("Structure: ${gridModel.rows} rows x ${gridModel.cols} columns. Total cells: ${gridModel.cells.size}")

        // Write raw cell contours dump to help debug missing top rows
        val rawContoursFile = File("$brainDir/stage5_raw_contours.txt")
        rawContoursFile.printWriter().use { out ->
            val subGridInvertedDebug = Mat()
            Core.bitwise_not(subGrid, subGridInvertedDebug)
            val debugContours = ArrayList<org.opencv.core.MatOfPoint>()
            Imgproc.findContours(subGridInvertedDebug, debugContours, Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
            out.println("Total raw contours found: ${debugContours.size}")
            out.println("Timetable rect width: ${timetableRect.width}, height: ${timetableRect.height}")
            out.println("Index | x | y | w | h | area")
            out.println("----------------------------")
            debugContours.forEachIndexed { idx, contour ->
                val r = Imgproc.boundingRect(contour)
                out.println("$idx | ${r.x} | ${r.y} | ${r.width} | ${r.height} | ${r.width * r.height}")
            }
            subGridInvertedDebug.release()
            debugContours.forEach { it.release() }
        }

        // Write grid details to a text file
        val gridDetailsFile = File("$brainDir/stage5_grid_details.txt")
        gridDetailsFile.printWriter().use { out ->
            out.println("Timetable Rect: x=${timetableRect.x}, y=${timetableRect.y}, w=${timetableRect.width}, h=${timetableRect.height}")
            out.println("Row Boundaries: $rowBoundaries")
            out.println("Col Boundaries: $colBoundaries")
            out.println("\nCells List:")
            out.println("Index | startRow | startCol | rowSpan | colSpan | rect.x | rect.y | rect.width | rect.height")
            out.println("-----------------------------------------------------------------------------------------")
            gridModel.cells.forEachIndexed { index, cell ->
                out.println("$index | ${cell.startRow} | ${cell.startCol} | ${cell.rowSpan} | ${cell.colSpan} | ${cell.rect.x} | ${cell.rect.y} | ${cell.rect.width} | ${cell.rect.height}")
            }
        }

        // Save individual cropped cell images
        val cellsDir = "$brainDir/extracted_cells"
        File(cellsDir).mkdirs()
        for (cell in gridModel.cells) {
            val crop = Mat(preprocessed.originalMat, cell.rect)
            val path = "$cellsDir/cell_row_${cell.startRow}_col_${cell.startCol}.png"
            Imgcodecs.imwrite(path, crop)
            crop.release()
        }
        println("Saved Stage 5 (Cropped Cells) to $cellsDir/")

        // 8. OCR Stage: Provide OCR texts corresponding to what ML Kit recognizes for these cell coordinates
        val recognizedCells = ArrayList<RecognizedCell>()
        for (cell in gridModel.cells) {
            val r = cell.startRow
            val c = cell.startCol
            val text = getExpectedOcrText(r, c)
            recognizedCells.add(
                RecognizedCell(
                    id = cell.id,
                    startRow = cell.startRow,
                    startCol = cell.startCol,
                    rowSpan = cell.rowSpan,
                    colSpan = cell.colSpan,
                    rect = cell.rect,
                    text = text,
                    confidence = 0.95f
                )
            )
        }

        // Print cell OCR results
        val ocrLog = File("$brainDir/stage6_ocr_results.txt")
        ocrLog.printWriter().use { out ->
            out.println("Row | Column | Confidence | Recognized Text")
            out.println("-------------------------------------------")
            for (cell in recognizedCells) {
                out.println("${cell.startRow} | ${cell.startCol} | ${cell.confidence} | ${cell.text}")
            }
        }
        println("Saved Stage 6 (OCR Results) to ${ocrLog.absolutePath}")

        // 9. Validation stage
        val validatedCells = recognizedCells.map { cell ->
            val cleaned = validationEngine.cleanSubjectText(cell.text)
            cell.copy(text = cleaned)
        }
        val valLog = File("$brainDir/stage7_validation_changes.txt")
        valLog.printWriter().use { out ->
            out.println("Row | Column | Original Text -> Validated Text")
            out.println("----------------------------------------------")
            for (i in recognizedCells.indices) {
                val orig = recognizedCells[i].text
                val clean = validatedCells[i].text
                if (orig != clean) {
                    out.println("${recognizedCells[i].startRow} | ${recognizedCells[i].startCol} | '$orig' -> '$clean'")
                }
            }
        }
        println("Saved Stage 7 (Validation changes) to ${valLog.absolutePath}")

        // 10. Semantic Parsing
        val parsingResult = semanticParser.parse(validatedCells)
        val jsonStr = semanticParser.toJson(parsingResult)
        
        val outJson = File("$brainDir/stage8_final_result.json")
        outJson.writeText(jsonStr)
        println("Saved Stage 8 (Final JSON result) to ${outJson.absolutePath}")

        // Clean up Mats
        subHoriz.release()
        subVert.release()
        subGrid.release()
        gridMasks.horizontal.release()
        gridMasks.vertical.release()
        gridMasks.combined.release()
        preprocessed.originalMat.release()
        preprocessed.grayMat.release()
        preprocessed.threshMat.release()
        originalMat.release()
    }

    private fun getExpectedOcrText(row: Int, col: Int): String {
        // Headers row 2
        if (row == 2) {
            return when (col) {
                0 -> "DAY"
                1 -> "12.00pm -\n12.50pm"
                2 -> "12.50pm - 1:\n40pm"
                3 -> "1:40pm - 2:\n30pm"
                4 -> "2:30pm - 3:\n00pm"
                5 -> "3:00pm - 3:\n50pm"
                6 -> "3:50pm - 4:\n40pm"
                7 -> "4:40pm - 5:\n30pm"
                8 -> "5:30pm - 6:\n00pm"
                else -> ""
            }
        }

        // Col 0: Weekdays
        if (col == 0) {
            return when (row) {
                3 -> "Mondav" // Intentional OCR typo to test ValidationEngine
                4 -> "Tuesday"
                5 -> "Wednesday"
                6 -> "Thursday"
                7 -> "Friday"
                else -> ""
            }
        }

        // Recess column 4 (spans rows 3-7)
        if (col == 4 && row in 3..7) return "RECESS"

        // Library hours column 8 (spans rows 3-7)
        if (col == 8 && row in 3..7) return "LIBRARY\nHOURS"

        // Monday (Row 3)
        if (row == 3) {
            return when (col) {
                1, 2, 3 -> "AI Lab (SB)"
                5 -> "Theory of\nComputation (AS)"
                7 -> "Aptitude and\nReasoning\nAbility (RK)"
                else -> ""
            }
        }

        // Tuesday (Row 4)
        if (row == 4) {
            return when (col) {
                1 -> "oop using javaa (ANM)" // Intentional OCR typo
                2 -> "Software Engineering (TB)"
                5 -> "computor networks ABG)" // Intentional typos
                6, 7 -> "Introduction to AI (SB)"
                else -> ""
            }
        }

        // Wednesday (Row 5)
        if (row == 5) {
            return when (col) {
                1, 2, 3 -> "Computer Networks Lab (ABG) LAB 21"
                5, 6, 7 -> "OOP using JAVA LAB (ANM)"
                else -> ""
            }
        }

        // Thursday (Row 6)
        if (row == 6) {
            return when (col) {
                7 -> "Software Engineering\n(TB)"
                else -> ""
            }
        }

        // Friday (Row 7)
        if (row == 7) {
            return when (col) {
                1 -> "Theory of Computation\n(AS)"
                2, 3 -> "computor networks (ABG)" // Intentional OCR typo
                5, 6, 7 -> "OOP using JAVA (ANM)"
                else -> ""
            }
        }

        return ""
    }
}
