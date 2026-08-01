package com.attendance.tracker.domain.validation

import com.attendance.tracker.domain.model.Schedule

/**
 * Validates domain constraints on weekly schedules.
 */
class ScheduleValidator {
    /**
     * Assesses whether a [Schedule] has valid timings.
     */
    fun validate(schedule: Schedule): ValidationResult {
        if (schedule.startTime.isAfter(schedule.endTime) || schedule.startTime == schedule.endTime) {
            return ValidationResult.Invalid("Start time must be strictly before end time")
        }
        return ValidationResult.Valid
    }
}
