package com.attendance.tracker.domain.validation

/**
 * Sealed class representing validation outcomes for business rules.
 */
sealed class ValidationResult {
    /**
     * Indicates that the checked model is valid and passes all business rules.
     */
    object Valid : ValidationResult()

    /**
     * Indicates validation failure.
     * @property reason Descriptive error code or localized message.
     */
    data class Invalid(val reason: String) : ValidationResult()
}
