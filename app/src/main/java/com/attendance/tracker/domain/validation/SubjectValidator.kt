package com.attendance.tracker.domain.validation

import com.attendance.tracker.domain.model.Subject

/**
 * Validates domain constraints on study subjects.
 */
class SubjectValidator {
    /**
     * Assesses whether a [Subject] satisfies business logic criteria.
     */
    fun validate(subject: Subject): ValidationResult {
        if (subject.name.isBlank()) {
            return ValidationResult.Invalid("Subject name cannot be empty")
        }
        if (subject.creditHours < 0) {
            return ValidationResult.Invalid("Credit hours cannot be negative")
        }
        return ValidationResult.Valid
    }
}
