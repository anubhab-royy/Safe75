package com.attendance.tracker.domain.usecase.attendance

import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.repository.AttendanceRepository
import com.attendance.tracker.domain.validation.AttendanceValidator
import com.attendance.tracker.domain.validation.ValidationResult
import javax.inject.Inject

/**
 * Domain Use Case to register/mark attendance for a class.
 * Enforces duplicate checks and field validation rules.
 */
class MarkAttendanceUseCase @Inject constructor(
    private val repository: AttendanceRepository,
    private val validator: AttendanceValidator
) {
    suspend operator fun invoke(attendance: Attendance): ValidationResult {
        val isDuplicate = if (attendance.id == 0L) {
            repository.checkDuplicateAttendance(attendance.subjectId, attendance.scheduleId, attendance.date)
        } else {
            false
        }
        
        val validation = validator.validate(attendance, isDuplicate)
        if (validation is ValidationResult.Valid) {
            repository.insertAttendance(attendance)
        }
        return validation
    }
}
