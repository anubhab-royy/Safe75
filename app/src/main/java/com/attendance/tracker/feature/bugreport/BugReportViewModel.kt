package com.attendance.tracker.feature.bugreport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.tracker.core.common.DispatcherProvider
import com.attendance.tracker.core.diagnostics.DiagnosticsExporter
import com.attendance.tracker.feature.bugreport.data.BugReportWorkScheduler
import com.attendance.tracker.feature.bugreport.data.ScreenshotContentReader
import com.attendance.tracker.feature.bugreport.domain.repository.BugReportQueueRepository
import com.attendance.tracker.feature.bugreport.model.ScreenshotAttachment
import com.attendance.tracker.feature.bugreport.model.ScreenshotConstraints
import com.attendance.tracker.feature.device.data.remote.NetworkError
import com.attendance.tracker.feature.device.domain.model.BugReportSubmission
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import javax.inject.Inject

@HiltViewModel
class BugReportViewModel @Inject constructor(
    private val queueRepository: BugReportQueueRepository,
    private val workScheduler: BugReportWorkScheduler,
    private val diagnosticsExporter: DiagnosticsExporter,
    private val screenshotContentReader: ScreenshotContentReader,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<BugReportUiState>(BugReportUiState.Editing())
    val uiState: StateFlow<BugReportUiState> = _uiState.asStateFlow()

    fun onDescriptionChanged(description: String) {
        val draft = draftFromState()
        if (draft != null) {
            _uiState.value = BugReportUiState.Editing(description, draft.screenshot)
        }
    }

    fun onScreenshotSelected(uri: String) {
        if (isBusy()) return
        val draft = draftFromState() ?: Draft()
        viewModelScope.launch {
            val result = withContext(dispatcherProvider.io) { screenshotContentReader.read(uri) }
            result.fold(
                onSuccess = { attachment ->
                    val validationMessage = validateScreenshot(attachment)
                    _uiState.value = if (validationMessage == null) {
                        BugReportUiState.Editing(draft.description, attachment)
                    } else {
                        BugReportUiState.ValidationError(
                            validationMessage,
                            draft.description,
                            draft.screenshot
                        )
                    }
                },
                onFailure = {
                    _uiState.value = BugReportUiState.ValidationError(
                        "The selected image could not be read.",
                        draft.description,
                        draft.screenshot
                    )
                }
            )
        }
    }

    fun removeScreenshot() {
        if (isBusy()) return
        val draft = draftFromState() ?: return
        _uiState.value = BugReportUiState.Editing(draft.description, null)
    }

    fun submit() {
        if (isBusy()) return
        val draft = draftFromState() ?: return
        validate(draft)?.let { message ->
            _uiState.value = BugReportUiState.ValidationError(
                message,
                draft.description,
                draft.screenshot
            )
            return
        }

        _uiState.value = BugReportUiState.Submitting(draft.description, draft.screenshot)
        viewModelScope.launch {
            try {
                val bundle = withContext(dispatcherProvider.io) { diagnosticsExporter.buildBundle() }
                val diagnosticsJson = withContext(dispatcherProvider.io) {
                    diagnosticsExporter.toJson(bundle)
                }
                val device = bundle.deviceInfo
                val submission = BugReportSubmission(
                    timestamp = bundle.exportedAt,
                    appVersion = device.appVersion,
                    versionCode = device.versionCode,
                    buildType = device.buildType,
                    androidVersion = device.androidVersion,
                    sdkVersion = device.sdkInt,
                    deviceManufacturer = device.manufacturer,
                    deviceModel = device.model,
                    cpuAbi = device.abi,
                    locale = device.locale,
                    crashReportId = bundle.crashReports.maxByOrNull { it.timestamp }?.id,
                    userDescription = draft.description,
                    diagnosticsMetadata = diagnosticsJson
                )

                val reportId = queueRepository.enqueue(submission, draft.screenshot).getOrElse {
                    throw it
                }
                workScheduler.enqueuePendingUploads()
                _uiState.value = BugReportUiState.Success(reportId.reportId)
            } catch (error: Exception) {
                _uiState.value = BugReportUiState.Failure(
                    message = friendlyMessage(error),
                    description = draft.description,
                    screenshot = draft.screenshot
                )
            }
        }
    }

    fun startNewReport() {
        _uiState.value = BugReportUiState.Editing()
    }

    private fun validate(draft: Draft): String? {
        val length = draft.description.trim().length
        if (length == 0) return "Describe the issue before submitting."
        if (length > MAX_DESCRIPTION_LENGTH) {
            return "Description must be $MAX_DESCRIPTION_LENGTH characters or fewer."
        }
        return draft.screenshot?.let(::validateScreenshot)
    }

    private fun validateScreenshot(attachment: ScreenshotAttachment): String? {
        if (attachment.contentType !in ScreenshotConstraints.acceptedContentTypes) {
            return "Screenshot must be a JPEG, PNG, or WEBP image."
        }
        if (attachment.bytes.size > ScreenshotConstraints.MAX_BYTES) {
            return "Screenshot must be 2 MB or smaller."
        }
        return null
    }

    private fun draftFromState(): Draft? {
        return when (val state = _uiState.value) {
            BugReportUiState.Idle -> Draft()
            is BugReportUiState.Editing -> Draft(state.description, state.screenshot)
            is BugReportUiState.Failure -> Draft(state.description, state.screenshot)
            is BugReportUiState.ValidationError -> Draft(state.description, state.screenshot)
            is BugReportUiState.Submitting -> Draft(state.description, state.screenshot)
            is BugReportUiState.UploadingScreenshot -> Draft(state.description, state.screenshot)
            is BugReportUiState.Success -> null
        }
    }

    private fun isBusy(): Boolean = when (_uiState.value) {
        is BugReportUiState.Submitting, is BugReportUiState.UploadingScreenshot -> true
        else -> false
    }

    private fun friendlyMessage(error: Throwable): String = when (error) {
        is NetworkError.EnrollmentMissing -> "Enroll this device before submitting a report."
        is NetworkError.UnknownHost, is NetworkError.Io -> "The network is unavailable. Check your connection and try again."
        is NetworkError.Timeout -> "The request timed out. Check your connection and try again."
        is NetworkError.Unauthorized, is NetworkError.Forbidden -> "This device is not authorized to submit reports."
        is NetworkError.Validation -> "The backend rejected the report details. Check the description and try again."
        is NetworkError.NotFound -> "The queued report could not be found by the backend."
        is NetworkError.Conflict -> "This report was already processed by the backend."
        is NetworkError.Serialization -> "The server returned an unreadable response."
        is NetworkError.ServerError -> "The reporting service is temporarily unavailable."
        is SerializationException -> "The diagnostics could not be serialized."
        is IOException -> "The selected image could not be read."
        else -> "The report could not be submitted. Please try again."
    }

    private data class Draft(
        val description: String = "",
        val screenshot: ScreenshotAttachment? = null
    )

    private companion object {
        const val MAX_DESCRIPTION_LENGTH = 1000
    }
}
