package com.attendance.tracker.feature.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.tracker.core.backup.BackupData
import com.attendance.tracker.core.backup.BackupFileProvider
import com.attendance.tracker.core.backup.ValidationResult
import com.attendance.tracker.domain.model.BackupResult
import com.attendance.tracker.domain.model.RestoreOptions
import com.attendance.tracker.domain.model.RestoreResult
import com.attendance.tracker.domain.usecase.backup.ExportBackupUseCase
import com.attendance.tracker.domain.usecase.backup.GetBackupDataUseCase
import com.attendance.tracker.domain.usecase.backup.GetLastBackupTimestampUseCase
import com.attendance.tracker.domain.usecase.backup.ImportBackupUseCase
import com.attendance.tracker.domain.usecase.backup.PreviewBackupUseCase
import com.attendance.tracker.domain.usecase.backup.ValidateBackupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI state for the Backup / Restore screens. */
data class BackupUiState(
    val isLoading: Boolean = false,
    val currentData: BackupData? = null,
    val previewData: BackupData? = null,
    val validationErrors: List<String> = emptyList(),
    val lastBackupTimestamp: Long = 0L,
    val exportResult: BackupResult? = null,
    val restoreResult: RestoreResult? = null,
    val error: String? = null,
    val showRestoreOptions: Boolean = false,
    val restoreOptions: RestoreOptions = RestoreOptions()
)

/**
 * ViewModel managing backup export, file preview, validation, and selective restore.
 * Shared by [BackupScreen] (export) and [RestoreScreen] (import flow).
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
    private val validateBackupUseCase: ValidateBackupUseCase,
    private val previewBackupUseCase: PreviewBackupUseCase,
    private val getBackupDataUseCase: GetBackupDataUseCase,
    private val getLastBackupTimestampUseCase: GetLastBackupTimestampUseCase,
    private val backupFileProvider: BackupFileProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        loadCurrentSnapshot()
        loadLastBackupTimestamp()
    }

    /** Builds a timestamped backup file name for the system file picker. */
    fun createBackupFileName(): String = backupFileProvider.suggestedExportFileName()

    /** Loads the current DB snapshot for size estimation in the UI. */
    fun loadCurrentSnapshot() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val data = runCatching { getBackupDataUseCase() }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                currentData = data.getOrNull(),
                error = data.exceptionOrNull()?.message
            )
        }
    }

    private fun loadLastBackupTimestamp() {
        viewModelScope.launch {
            val timestamp = runCatching { getLastBackupTimestampUseCase() }.getOrDefault(0L)
            _uiState.value = _uiState.value.copy(lastBackupTimestamp = timestamp)
        }
    }

    /** Exports the full database to the chosen [destinationUri]. */
    fun exportBackup(destinationUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, exportResult = null, error = null)
            val result = exportBackupUseCase(destinationUri)
            _uiState.value = _uiState.value.copy(isLoading = false, exportResult = result)
            loadLastBackupTimestamp()
        }
    }

    /** Reads and validates a backup file — shows preview without committing. */
    fun previewBackup(sourceUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, validationErrors = emptyList(), previewData = null)
            val validResult = validateBackupUseCase(sourceUri)
            when (validResult) {
                is ValidationResult.Valid -> {
                    val preview = previewBackupUseCase(sourceUri)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        previewData = preview.getOrNull(),
                        showRestoreOptions = preview.isSuccess,
                        error = preview.exceptionOrNull()?.message
                    )
                }
                is ValidationResult.Invalid -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        validationErrors = validResult.errors,
                        showRestoreOptions = false
                    )
                }
            }
        }
    }

    /** Commits the restore with the selected [options]. */
    fun importBackup(sourceUri: Uri, options: RestoreOptions) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, restoreResult = null, error = null)
            val result = importBackupUseCase(sourceUri, options)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                restoreResult = result,
                showRestoreOptions = false,
                previewData = null
            )
        }
    }

    fun updateRestoreOptions(options: RestoreOptions) {
        _uiState.value = _uiState.value.copy(restoreOptions = options)
    }

    /** Advances from the preview dialog to the restore-options dialog. */
    fun goToRestoreOptions() {
        _uiState.value = _uiState.value.copy(showRestoreOptions = true)
    }

    fun dismissRestoreDialog() {
        _uiState.value = _uiState.value.copy(showRestoreOptions = false, previewData = null)
    }

    fun clearResults() {
        _uiState.value = _uiState.value.copy(exportResult = null, restoreResult = null, error = null)
    }
}
