package com.attendance.tracker.domain.validation

import com.attendance.tracker.domain.model.Attendance
import java.time.LocalDate
import javax.inject.Inject

/**
 * Validates domain constraints on individual attendance logs.
 */
class AttendanceValidator @Inject constructor() {

    /**
     * Assesses whether an [Attendance] satisfies calendar, subject, and duplicate logging constraints.
     */
    fun validate(attendance: Attendance, isDuplicate: Boolean): ValidationResult {
        if (attendance.subjectId <= 0L) {
            return ValidationResult.Invalid("Subject must exist")
        }
        if (attendance.scheduleId <= 0L) {
            return ValidationResult.Invalid("Schedule must exist")
        }
        if (attendance.date.isAfter(LocalDate.now())) {
            return ValidationResult.Invalid("Attendance date cannot be in the future")
        }
        if (isDuplicate) {
            return ValidationResult.Invalid("Attendance has already been marked for this class slot on this date")
        }
        return ValidationResult.Valid
    }
}
