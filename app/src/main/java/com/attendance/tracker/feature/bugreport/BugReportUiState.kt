package com.attendance.tracker.feature.bugreport

import com.attendance.tracker.feature.bugreport.model.ScreenshotAttachment

sealed interface BugReportUiState {
    data object Idle : BugReportUiState

    data class Editing(
        val description: String = "",
        val screenshot: ScreenshotAttachment? = null
    ) : BugReportUiState

    data class Submitting(
        val description: String,
        val screenshot: ScreenshotAttachment?
    ) : BugReportUiState

    data class UploadingScreenshot(
        val reportId: String,
        val description: String,
        val screenshot: ScreenshotAttachment
    ) : BugReportUiState

    data class Success(val reportId: String) : BugReportUiState

    data class Failure(
        val message: String,
        val description: String,
        val screenshot: ScreenshotAttachment?
    ) : BugReportUiState

    data class ValidationError(
        val message: String,
        val description: String,
        val screenshot: ScreenshotAttachment?
    ) : BugReportUiState
}
