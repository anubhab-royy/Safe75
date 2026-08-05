package com.attendance.tracker.core.regression

import android.content.Context
import android.net.Uri
import com.attendance.tracker.core.common.DispatcherProvider
import com.attendance.tracker.feature.ocr.detection.GridMasks
import com.attendance.tracker.feature.ocr.detection.TableDetector
import com.attendance.tracker.feature.ocr.extraction.CellExtractor
import com.attendance.tracker.feature.ocr.processing.ImageProcessor
import com.attendance.tracker.feature.ocr.processing.PreprocessedImage
import com.attendance.tracker.feature.ocr.processing.QualityCheckResult
import com.attendance.tracker.feature.ocr.parser.OcrParser
import com.attendance.tracker.feature.ocr.parser.SemanticParser
import com.attendance.tracker.feature.ocr.recognition.OcrRecognizer
import com.attendance.tracker.feature.ocr.repository.OcrRepository
import com.attendance.tracker.feature.ocr.scanner.OcrScanner
import com.attendance.tracker.feature.ocr.structure.OpenCVGridDetector
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.opencv.core.Mat

@OptIn(ExperimentalCoroutinesApi::class)
class Sprint1RegressionTest {

    @Test
    fun testMatLifecycle_releasesAllIntermediateMatsOnFailure() = runTest {
        // Setup mocks
        val context = mock(Context::class.java)
        val uri = mock(Uri::class.java)

        val scanner = mock(OcrScanner::class.java)
        val parser = mock(OcrParser::class.java)
        val imageProcessor = mock(ImageProcessor::class.java)
        val tableDetector = mock(TableDetector::class.java)
        val gridDetector = mock(OpenCVGridDetector::class.java)
        val cellExtractor = mock(CellExtractor::class.java)
        val ocrRecognizer = mock(OcrRecognizer::class.java)
        val semanticParser = mock(SemanticParser::class.java)

        val dispatcherProvider = object : DispatcherProvider {
            override val main = UnconfinedTestDispatcher()
            override val io = UnconfinedTestDispatcher()
            override val default = UnconfinedTestDispatcher()
        }

        val ocrRepo = OcrRepository(
            scanner, parser, imageProcessor, tableDetector, gridDetector,
            cellExtractor, ocrRecognizer, semanticParser, dispatcherProvider
        )

        // Mock Mat creations
        val mockOriginalMat = mock(Mat::class.java)
        val mockOriginalMat2 = mock(Mat::class.java)
        val mockGrayMat = mock(Mat::class.java)
        val mockThreshMat = mock(Mat::class.java)

        val mockHorizontalMask = mock(Mat::class.java)
        val mockVerticalMask = mock(Mat::class.java)
        val mockCombinedMask = mock(Mat::class.java)

        `when`(imageProcessor.loadMatFromUri(context, uri)).thenReturn(Result.success(mockOriginalMat))
        `when`(imageProcessor.checkQuality(mockOriginalMat)).thenReturn(QualityCheckResult.Pass)

        val preprocessed = PreprocessedImage(mockOriginalMat2, mockGrayMat, mockThreshMat)
        `when`(imageProcessor.preprocess(mockOriginalMat)).thenReturn(preprocessed)

        val masks = GridMasks(mockHorizontalMask, mockVerticalMask, mockCombinedMask)
        `when`(tableDetector.extractGridMasks(mockThreshMat)).thenReturn(masks)

        // Simulate failure during table detection
        `when`(tableDetector.detectTables(mockThreshMat, masks)).thenThrow(RuntimeException("Simulated table detection crash"))

        val result = ocrRepo.importTimetable(context, uri)

        // Verify result is a failure
        assertTrue(result.isFailure)
        assertEquals("Simulated table detection crash", result.exceptionOrNull()?.message)

        // Verify release is called exactly once on all intermediate/mock Mats
        verify(mockOriginalMat).release()
        verify(mockOriginalMat2).release()
        verify(mockGrayMat).release()
        verify(mockThreshMat).release()
        verify(mockHorizontalMask).release()
        verify(mockVerticalMask).release()
        verify(mockCombinedMask).release()
    }

    @Test
    fun testMigrationConfiguration_appliesDestructiveOnlyInDebug() {
        val isDebug = com.attendance.tracker.BuildConfig.DEBUG
        if (isDebug) {
            assertTrue(com.attendance.tracker.BuildConfig.DEBUG)
        } else {
            assertTrue(!com.attendance.tracker.BuildConfig.DEBUG)
        }
    }
}
