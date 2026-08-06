package com.attendance.tracker.feature.ocr.diagnostics

import com.attendance.tracker.core.logger.Logger

/**
 * Lightweight, removable OCR pipeline instrumentation (Phase B2).
 *
 * Records per-stage runtime evidence for the timetable OCR pipeline using the
 * existing [Logger] / diagnostics infrastructure so that debug and installed
 * release runs can be compared offline via `files/diagnostics/logs.json`.
 *
 * Every call is a no-op when [ENABLED] is false, so the instrumentation can be
 * disabled (or removed entirely) without touching any pipeline logic. The logs
 * emitted here never alter OCR behavior, allocate bitmaps, or re-process images.
 */
object OcrInstrumentation {

    const val ENABLED = true

    const val TAG_DECODE = "OcrDecode"
    const val TAG_QUALITY = "OcrQuality"
    const val TAG_PREPROCESS = "OcrPreprocess"
    const val TAG_TABLE = "OcrTable"
    const val TAG_GRID = "OcrGrid"
    const val TAG_CELL_EXTRACT = "OcrCellExtract"
    const val TAG_CELL_OCR = "OcrCellOcr"
    const val TAG_PIPELINE = "OcrPipeline"
    const val TAG_SUMMARY = "OcrSummary"
    const val TAG_FAILURE = "OcrFailure"

    fun i(tag: String, message: String) {
        if (ENABLED) Logger.i(tag, message)
    }

    fun d(tag: String, message: String) {
        if (ENABLED) Logger.d(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (ENABLED) Logger.e(tag, message, throwable)
    }

    /** Milliseconds elapsed since a [System.nanoTime] start timestamp. */
    fun elapsedMs(startNanos: Long): Long = (System.nanoTime() - startNanos) / 1_000_000
}
