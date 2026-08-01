package com.attendance.tracker.domain.validation

import com.attendance.tracker.domain.model.AttendanceRecord
import java.time.LocalDate

/**
 * Validates domain constraints on individual attendance logs.
 */
class AttendanceValidator {
    /**
     * Assesses whether an [AttendanceRecord] satisfies calendar and logging constraints.
     */
    fun validate(record: AttendanceRecord): ValidationResult {
        if (record.date.isAfter(LocalDate.now())) {
            return ValidationResult.Invalid("Attendance date cannot be in the future")
        }
        return ValidationResult.Valid
    }
}
