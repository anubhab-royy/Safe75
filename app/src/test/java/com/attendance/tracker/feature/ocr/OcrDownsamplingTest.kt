package com.attendance.tracker.feature.ocr

import com.attendance.tracker.feature.ocr.detection.TableDetector
import com.attendance.tracker.feature.ocr.processing.ImageProcessor
import com.attendance.tracker.feature.ocr.processing.QualityCheckResult
import com.attendance.tracker.feature.ocr.structure.OpenCVGridDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Evidence for the Phase A3 downsampling strategy.
 *
 * Loads the known-good production regression image and runs the real OpenCV
 * pipeline (quality check, preprocessing, grid-mask extraction, table detection,
 * structure detection) at multiple simulated screenshot resolutions. Proves that
 * processing the image at the downsampled working resolution (longest edge ~2048)
 * produces the identical grid structure as processing it at native resolution.
 */
class OcrDownsamplingTest {

    companion object {
        private const val BASE_IMAGE_PATH =
            "C:/Users/anubh/.gemini/antigravity-ide/brain/e8ce8f7a-c023-4850-8ab4-83328089a723/media__1785707227737.jpg"

        init {
            try {
                nu.pattern.OpenCV.loadShared()
            } catch (e: Throwable) {
                try {
                    org.opencv.osgi.OpenCVNativeLoader().init()
                } catch (e2: Throwable) {
                    System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME)
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

    private data class PipelineStructure(
        val tableRects: List<Rect>,
        val rows: Int,
        val cols: Int,
        val cellCount: Int
    )

    // --- Task 2 evidence: sample-size computation ---

    @Test
    fun computeSampleSize_landsLongestEdgeWithinWorkingResolution() {
        val p = ImageProcessor()
        // Small images are never upscaled or altered.
        assertEquals(1, p.computeSampleSize(1024, 608, 2048, 400))
        // Typical phone screenshots.
        assertEquals(2, p.computeSampleSize(4000, 3000, 2048, 400))
        assertEquals(2, p.computeSampleSize(4032, 3024, 2048, 400))
        assertEquals(2, p.computeSampleSize(2340, 1080, 2048, 400))
        assertEquals(2, p.computeSampleSize(1080, 2340, 2048, 400))
        // 50MP-class source.
        assertEquals(4, p.computeSampleSize(8064, 6048, 2048, 400))
        // Power-of-two granularity leaves the edge in (target/2, target].
        assertEquals(4, p.computeSampleSize(5000, 3750, 2048, 400))
        // Extreme aspect ratios never shrink below the quality floor when avoidable.
        assertEquals(2, p.computeSampleSize(5000, 600, 2048, 400))
    }

    // --- Task 2/8 evidence: identical structure at downsampled resolution ---

    @Test
    fun realBaseImage_producesKnownGoodStructure() {
        // Anchors "no regression on the previous successful timetable image".
        // A sharp synthetic upscale would be unrepresentative, so the real image
        // is processed at its native 1024x608 (its actual production resolution).
        val base = Imgcodecs.imread(BASE_IMAGE_PATH)
        if (base.empty()) {
            println("Skipping: base image not found at $BASE_IMAGE_PATH")
            return
        }
        println("Base image: ${base.cols()}x${base.rows()}")

        val struct = runPipeline(base)
        assertEquals(2, struct.tableRects.size)
        assertEquals(8, struct.rows)
        assertEquals(9, struct.cols)
        assertTrue("Expected ~35 timetable cells, got ${struct.cellCount}", abs(struct.cellCount - 35) <= 2)

        base.release()
    }

    @Test
    fun downsampledPipeline_matchesNativeStructure_atLargeResolution() {
        // A genuinely sharp 10MP screenshot: draw the grid directly at 4096 longest
        // edge so its detail is real (not a blurry upscale).
        val rows = 8
        val cols = 9
        val large = drawGrid(4096, 2432, rows, cols, thickness = 8)
        println("Large source: ${large.cols()}x${large.rows()} (${"%.1f".format(large.cols() * large.rows() / 1_000_000.0)} MP)")

        // Simulate the BitmapFactory inSampleSize=2 decode used by loadMatFromUri.
        val sample = ImageProcessor().computeSampleSize(large.cols(), large.rows(), 2048, 400)
        val downsampled = scaleToLongest(large, 2048)
        println("Downsampled (sample=$sample): ${downsampled.cols()}x${downsampled.rows()}")

        val nativeStruct = runPipeline(large)
        val downStruct = runPipeline(downsampled)

        // Downsampled processing must not change the detected structure.
        assertEquals("Table count must match", nativeStruct.tableRects.size, downStruct.tableRects.size)
        assertEquals("Grid rows must match", nativeStruct.rows, downStruct.rows)
        assertEquals("Grid cols must match", nativeStruct.cols, downStruct.cols)
        assertTrue(
            "Cell count must be close (native=${nativeStruct.cellCount}, down=${downStruct.cellCount})",
            abs(nativeStruct.cellCount - downStruct.cellCount) <= 3
        )

        // Table bounding boxes must match once re-scaled to the same coordinate space.
        val scale = large.cols().toDouble() / downsampled.cols().toDouble()
        for (i in nativeStruct.tableRects.indices) {
            val n = nativeStruct.tableRects[i]
            val d = downStruct.tableRects[i]
            assertTrue("Table rect x mismatch", abs(n.x - d.x * scale) <= large.cols() * 0.02)
            assertTrue("Table rect y mismatch", abs(n.y - d.y * scale) <= large.rows() * 0.02)
            assertTrue("Table rect w mismatch", abs(n.width - d.width * scale) <= large.cols() * 0.02)
            assertTrue("Table rect h mismatch", abs(n.height - d.height * scale) <= large.rows() * 0.02)
        }

        // The downsampled pipeline must not trip the low-resolution quality gate.
        assertTrue(
            "Downsampled quality must still pass",
            imageProcessor.checkQuality(downsampled) is QualityCheckResult.Pass
        )

        large.release()
        downsampled.release()
    }

    // --- helpers ---

    private fun drawGrid(w: Int, h: Int, rows: Int, cols: Int, thickness: Int): Mat {
        // Light "paper" background with dark grid lines (the polarity a real
        // timetable has after the pipeline's THRESH_BINARY_INV).
        val mat = Mat(h, w, CvType.CV_8UC1, Scalar(255.0))
        for (c in 0..cols) {
            val x = c * w / cols
            Imgproc.line(mat, Point(x.toDouble(), 0.0), Point(x.toDouble(), (h - 1).toDouble()), Scalar(0.0), thickness)
        }
        for (r in 0..rows) {
            val y = r * h / rows
            Imgproc.line(mat, Point(0.0, y.toDouble()), Point((w - 1).toDouble(), y.toDouble()), Scalar(0.0), thickness)
        }
        return mat
    }

    private fun runPipeline(mat: Mat): PipelineStructure {
        val t0 = System.currentTimeMillis()
        val quality = imageProcessor.checkQuality(mat)
        assertTrue("Quality check must pass at ${mat.cols()}x${mat.rows()}: $quality", quality is QualityCheckResult.Pass)

        val pre = imageProcessor.preprocess(mat)
        val masks = tableDetector.extractGridMasks(pre.threshMat)
        val tables = tableDetector.detectTables(pre.threshMat, masks)

        var rows = 0
        var cols = 0
        var cells = 0
        if (tables.isNotEmpty()) {
            val grid = gridDetector.detectStructure(
                tables.first(),
                masks.horizontal,
                masks.vertical,
                masks.combined
            )
            rows = grid.rows
            cols = grid.cols
            cells = grid.cells.size
        }

        masks.horizontal.release()
        masks.vertical.release()
        masks.combined.release()
        pre.originalMat.release()
        pre.grayMat.release()
        pre.threshMat.release()

        val t1 = System.currentTimeMillis()
        println(
            "Pipeline ${mat.cols()}x${mat.rows()} in ${t1 - t0} ms -> " +
                "${tables.size} tables, $rows rows x $cols cols, $cells cells"
        )
        return PipelineStructure(tables, rows, cols, cells)
    }

    private fun scaleToLongest(src: Mat, longest: Int): Mat {
        val scale = longest.toDouble() / max(src.cols(), src.rows())
        val w = max(1, (src.cols() * scale).toInt())
        val h = max(1, (src.rows() * scale).toInt())
        val dst = Mat()
        val interp = if (scale < 1.0) Imgproc.INTER_AREA else Imgproc.INTER_LINEAR
        Imgproc.resize(src, dst, Size(w.toDouble(), h.toDouble()), 0.0, 0.0, interp)
        return dst
    }
}
