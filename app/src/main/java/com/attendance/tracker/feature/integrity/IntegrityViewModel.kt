package com.attendance.tracker.feature.integrity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.tracker.domain.model.IntegrityReport
import com.attendance.tracker.domain.usecase.backup.CheckDataIntegrityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the data-integrity verification screen. */
data class IntegrityUiState(
    val isLoading: Boolean = false,
    val report: IntegrityReport? = null,
    val error: String? = null
)

/**
 * ViewModel that runs a read-only data-integrity scan of the local database
 * and exposes the resulting [IntegrityReport].
 */
@HiltViewModel
class IntegrityViewModel @Inject constructor(
    private val checkDataIntegrityUseCase: CheckDataIntegrityUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(IntegrityUiState())
    val uiState: StateFlow<IntegrityUiState> = _uiState.asStateFlow()

    init {
        runCheck()
    }

    /** Runs the integrity scan again (e.g. after a repair). */
    fun runCheck() {
        viewModelScope.launch {
            _uiState.value = IntegrityUiState(isLoading = true, report = _uiState.value.report)
            _uiState.value = try {
                IntegrityUiState(isLoading = false, report = checkDataIntegrityUseCase())
            } catch (e: Exception) {
                IntegrityUiState(isLoading = false, error = e.message ?: "Integrity check failed.")
            }
        }
    }
}
