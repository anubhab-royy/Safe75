package com.attendance.tracker.domain.validation

import com.attendance.tracker.domain.model.Semester

/**
 * Validates domain constraints on semesters.
 */
class SemesterValidator {
    /**
     * Assesses whether a [Semester] has valid date ranges.
     */
    fun validate(semester: Semester): ValidationResult {
        if (semester.name.isBlank()) {
            return ValidationResult.Invalid("Semester name cannot be empty")
        }
        if (semester.startDate.isAfter(semester.endDate) || semester.startDate == semester.endDate) {
            return ValidationResult.Invalid("Start date must be strictly before end date")
        }
        return ValidationResult.Valid
    }
}
