package com.attendance.tracker.core.common

/**
 * Sealed class representing application-wide error states, subclassing Exception.
 */
sealed class AppError(
    val msg: String,
    val throwable: Throwable? = null
) : Exception(msg, throwable) {
    
    /**
     * Errors originating from Room DB queries or migrations.
     */
    class DatabaseError(message: String, cause: Throwable? = null) : AppError(message, cause)

    /**
     * Errors originating from business rule validation failures.
     */
    class ValidationError(message: String, cause: Throwable? = null) : AppError(message, cause)

    /**
     * Errors originating from network requests or backup synchronization.
     */
    class NetworkError(message: String, cause: Throwable? = null) : AppError(message, cause)

    /**
     * Unhandled or unknown runtime exceptions.
     */
    class UnknownError(message: String, cause: Throwable? = null) : AppError(message, cause)
}
