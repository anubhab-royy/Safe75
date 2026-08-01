package com.attendance.tracker.core.common

/**
 * A generic UI State wrapper commonly used in ViewModels to expose status to Composables.
 */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<out T>(val data: T) : UiState<T>()
    data class Error(val message: String? = null, val throwable: Throwable? = null) : UiState<Nothing>()
    object Empty : UiState<Nothing>()
}
