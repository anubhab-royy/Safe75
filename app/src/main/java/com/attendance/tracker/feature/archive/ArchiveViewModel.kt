package com.attendance.tracker.feature.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.attendance.tracker.domain.model.ArchiveData
import com.attendance.tracker.domain.model.RestoreOptions
import com.attendance.tracker.domain.model.RestoreResult
import com.attendance.tracker.domain.usecase.backup.CreateArchiveUseCase
import com.attendance.tracker.domain.usecase.backup.DeleteArchiveUseCase
import com.attendance.tracker.domain.usecase.backup.GetArchiveUseCase
import com.attendance.tracker.domain.usecase.backup.GetArchivesUseCase
import com.attendance.tracker.domain.usecase.backup.RestoreArchiveUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** UI state for the archive browser and details screens. */
data class ArchiveUiState(
    val isLoading: Boolean = false,
    val selectedArchive: ArchiveData? = null,
    val restoreResult: RestoreResult? = null,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val showDeleteConfirm: Long? = null,   // archive id to delete, or null
    val showRestoreOptions: Boolean = false,
    val restoreOptions: RestoreOptions = RestoreOptions(restoreSettings = false)
)

/**
 * ViewModel managing archive list, create, delete, and restore operations.
 */
@HiltViewModel
class ArchiveViewModel @Inject constructor(
    getArchivesUseCase: GetArchivesUseCase,
    private val createArchiveUseCase: CreateArchiveUseCase,
    private val deleteArchiveUseCase: DeleteArchiveUseCase,
    private val getArchiveUseCase: GetArchiveUseCase,
    private val restoreArchiveUseCase: RestoreArchiveUseCase
) : ViewModel() {

    /** Observed list of all archived semesters. */
    val archives: StateFlow<List<ArchiveData>> = getArchivesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(ArchiveUiState())
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    /** Opens the create archive dialog. */
    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }

    fun dismissCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    /** Creates a new archive snapshot. */
    fun createArchive(name: String, startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { createArchiveUseCase(name, startDate, endDate) }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false, showCreateDialog = false)
                }
        }
    }

    /** Loads full archive details including per-subject stats. */
    fun loadArchiveDetails(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val archive = getArchiveUseCase(id)
            _uiState.value = _uiState.value.copy(isLoading = false, selectedArchive = archive)
        }
    }

    /** Shows the delete confirmation for a specific archive. */
    fun requestDelete(id: Long) {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = id)
    }

    fun dismissDelete() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = null)
    }

    /** Permanently deletes an archive. */
    fun deleteArchive(id: Long) {
        viewModelScope.launch {
            deleteArchiveUseCase(id)
            _uiState.value = _uiState.value.copy(showDeleteConfirm = null)
        }
    }

    /** Shows restore options for the currently selected archive. */
    fun showRestoreOptions() {
        _uiState.value = _uiState.value.copy(showRestoreOptions = true)
    }

    fun dismissRestoreOptions() {
        _uiState.value = _uiState.value.copy(showRestoreOptions = false)
    }

    fun updateRestoreOptions(options: RestoreOptions) {
        _uiState.value = _uiState.value.copy(restoreOptions = options)
    }

    /** Restores the currently selected archive. */
    fun restoreArchive(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, restoreResult = null)
            val result = restoreArchiveUseCase(id, _uiState.value.restoreOptions)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                restoreResult = result,
                showRestoreOptions = false
            )
        }
    }

    fun clearRestoreResult() {
        _uiState.value = _uiState.value.copy(restoreResult = null)
    }
}
