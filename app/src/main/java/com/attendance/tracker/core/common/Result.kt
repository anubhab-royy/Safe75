package com.attendance.tracker.core.common

/**
 * A sealed class representing the outcome of data-fetching operations.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
