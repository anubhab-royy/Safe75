package com.attendance.tracker.domain.validation

import com.attendance.tracker.domain.model.Schedule
import javax.inject.Inject

/**
 * Validates domain constraints on weekly schedules.
 */
class ScheduleValidator @Inject constructor() {
    /**
     * Assesses whether a [Schedule] has valid timings and fields.
     */
    fun validate(schedule: Schedule, existingSchedules: List<Schedule>): ValidationResult {
        if (schedule.subjectId <= 0L) {
            return ValidationResult.Invalid("Subject must exist")
        }
        if (schedule.versionId <= 0L) {
            return ValidationResult.Invalid("Version required")
        }
        if (schedule.startTime.isAfter(schedule.endTime) || schedule.startTime == schedule.endTime) {
            return ValidationResult.Invalid("Start time must be strictly before end time")
        }
        
        // Local overlap validation on client side
        val hasOverlap = existingSchedules.any { existing ->
            existing.id != schedule.id &&
            existing.versionId == schedule.versionId &&
            existing.dayOfWeek == schedule.dayOfWeek &&
            schedule.startTime.isBefore(existing.endTime) &&
            schedule.endTime.isAfter(existing.startTime)
        }
        
        if (hasOverlap) {
            return ValidationResult.Invalid("Schedule overlaps with an existing class")
        }

        schedule.room?.let {
            if (it.length > 100) {
                return ValidationResult.Invalid("Room location name cannot exceed 100 characters")
            }
        }

        schedule.teacherOverride?.let {
            if (it.length > 100) {
                return ValidationResult.Invalid("Teacher name cannot exceed 100 characters")
            }
        }

        return ValidationResult.Valid
    }
}
