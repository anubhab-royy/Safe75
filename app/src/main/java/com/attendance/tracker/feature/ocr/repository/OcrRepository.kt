package com.attendance.tracker.feature.ocr.repository

import android.content.Context
import android.net.Uri
import com.attendance.tracker.feature.ocr.model.OcrTimetableRow
import com.attendance.tracker.feature.ocr.model.OcrAttendanceRow
import com.attendance.tracker.feature.ocr.scanner.OcrScanner
import com.attendance.tracker.feature.ocr.parser.OcrParser
import javax.inject.Inject

/**
 * Coordinate image text extraction and data parsers.
 */
class OcrRepository @Inject constructor(
    private val scanner: OcrScanner,
    private val parser: OcrParser
) {
    /**
     * Scans an image and extracts timetable class rows.
     */
    suspend fun importTimetable(context: Context, uri: Uri): Result<List<OcrTimetableRow>> {
        val scanResult = scanner.scanImage(context, uri)
        return scanResult.map { parser.parseTimetable(it) }
    }

    /**
     * Scans an image and extracts attendance records.
     */
    suspend fun importAttendance(context: Context, uri: Uri): Result<List<OcrAttendanceRow>> {
        val scanResult = scanner.scanImage(context, uri)
        return scanResult.map { parser.parseAttendance(it) }
    }
}
