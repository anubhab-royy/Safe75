package com.attendance.tracker.feature.ocr.model

/**
 * UI-layer representation of scanned OCR elements prior to database persistence.
 */
data class OcrUiModel(
    val rawText: String,
    val identifiedSubjects: List<String>
)
