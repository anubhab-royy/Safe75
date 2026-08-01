package com.attendance.tracker.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.tracker.domain.model.ResetOptions
import com.attendance.tracker.domain.model.ResetPreview
import com.attendance.tracker.domain.model.ResetResult
import com.attendance.tracker.domain.usecase.backup.GetResetPreviewUseCase
import com.attendance.tracker.domain.usecase.backup.SemesterResetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the semester reset wizard. */
data class SemesterResetUiState(
    val isLoading: Boolean = false,
    val options: ResetOptions = ResetOptions(),
    val confirmationText: String = "",
    val resetPreview: ResetPreview? = null,
    val resetResult: ResetResult? = null,
    val error: String? = null,
    val currentStep: Int = 0   // 0 = options, 1 = preview, 2 = confirm
)

/**
 * ViewModel for the multi-step semester reset wizard.
 */
@HiltViewModel
class SemesterViewModel @Inject constructor(
    private val semesterResetUseCase: SemesterResetUseCase,
    private val getResetPreviewUseCase: GetResetPreviewUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SemesterResetUiState())
    val uiState: StateFlow<SemesterResetUiState> = _uiState.asStateFlow()

    fun updateOptions(options: ResetOptions) {
        _uiState.value = _uiState.value.copy(options = options)
    }

    fun updateConfirmationText(text: String) {
        _uiState.value = _uiState.value.copy(confirmationText = text)
    }

    fun goToStep(step: Int) {
        _uiState.value = _uiState.value.copy(currentStep = step.coerceIn(0, 2))
        if (step == 1) {
            loadResetPreview()
        }
    }

    /** Loads current record counts to preview the impact of the reset. */
    private fun loadResetPreview() {
        viewModelScope.launch {
            val preview = runCatching { getResetPreviewUseCase() }
            _uiState.value = _uiState.value.copy(
                resetPreview = preview.getOrNull(),
                error = preview.exceptionOrNull()?.message
            )
        }
    }

    val isConfirmEnabled: Boolean
        get() = _uiState.value.confirmationText.trim().uppercase() == "RESET"

    fun executeReset() {
        if (!isConfirmEnabled) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = semesterResetUseCase(_uiState.value.options)
            _uiState.value = _uiState.value.copy(isLoading = false, resetResult = result)
        }
    }

    fun clearResult() {
        _uiState.value = _uiState.value.copy(resetResult = null, confirmationText = "", currentStep = 0)
    }
}
