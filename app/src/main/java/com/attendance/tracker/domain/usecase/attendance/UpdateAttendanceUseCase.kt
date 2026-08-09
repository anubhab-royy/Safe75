package com.attendance.tracker.domain.usecase.attendance

import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.repository.AttendanceRepository
import com.attendance.tracker.domain.validation.AttendanceValidator
import com.attendance.tracker.domain.validation.ValidationResult
import javax.inject.Inject

/**
 * Domain Use Case to modify an existing attendance log.
 */
class UpdateAttendanceUseCase @Inject constructor(
    private val repository: AttendanceRepository,
    private val validator: AttendanceValidator
) {
    suspend operator fun invoke(attendance: Attendance): ValidationResult {
        // Exclude self from duplicate check during edits
        val existing = repository.getAttendanceById(attendance.id)
        val isDuplicate = if (existing != null && (existing.subjectId != attendance.subjectId || existing.scheduleId != attendance.scheduleId || existing.date != attendance.date)) {
            repository.checkDuplicateAttendance(attendance.subjectId, attendance.scheduleId, attendance.date)
        } else {
            false
        }

        val validation = validator.validate(attendance, isDuplicate)
        if (validation is ValidationResult.Valid) {
            val preservedAttendance = existing?.let {
                attendance.copy(
                    createdAt = it.createdAt,
                    updatedAt = System.currentTimeMillis()
                )
            } ?: attendance
            val updatedRows = repository.updateAttendance(preservedAttendance)
            if (updatedRows == 0) {
                return ValidationResult.Invalid("Attendance record was not found")
            }
        }
        return validation
    }
}
