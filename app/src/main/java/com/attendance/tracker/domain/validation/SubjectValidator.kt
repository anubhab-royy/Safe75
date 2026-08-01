package com.attendance.tracker.domain.validation

import com.attendance.tracker.domain.model.Subject
import javax.inject.Inject

/**
 * Validates domain constraints on study subjects.
 */
class SubjectValidator @Inject constructor() {
    /**
     * Assesses whether a [Subject] satisfies business logic criteria.
     * Checks name presence, uniqueness, length limits, and target percentages.
     */
    fun validate(subject: Subject, existingSubjects: List<Subject>): ValidationResult {
        if (subject.name.isBlank()) {
            return ValidationResult.Invalid("Subject name is required")
        }
        if (subject.name.length > 100) {
            return ValidationResult.Invalid("Subject name cannot exceed 100 characters")
        }
        
        // Check uniqueness (ignoring casing and matching subject IDs for updates)
        val isDuplicate = existingSubjects.any { 
            it.id != subject.id && it.name.trim().equals(subject.name.trim(), ignoreCase = true) 
        }
        if (isDuplicate) {
            return ValidationResult.Invalid("Subject name must be unique")
        }

        if (subject.requiredAttendancePercentage !in 1..100) {
            return ValidationResult.Invalid("Required attendance must be between 1 and 100%")
        }

        if (subject.personalAttendanceGoal !in subject.requiredAttendancePercentage..100) {
            return ValidationResult.Invalid("Personal goal must be between required attendance and 100%")
        }

        subject.facultyName?.let {
            if (it.length > 100) {
                return ValidationResult.Invalid("Faculty name cannot exceed 100 characters")
            }
        }

        return ValidationResult.Valid
    }
}
