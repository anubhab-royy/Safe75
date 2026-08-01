package com.attendance.tracker.domain.usecase.attendance

import com.attendance.tracker.domain.repository.AttendanceRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Domain Use Case to verify if an attendance entry exists for a specific day and class slot.
 */
class CheckDuplicateAttendanceUseCase @Inject constructor(
    private val repository: AttendanceRepository
) {
    suspend operator fun invoke(subjectId: Long, scheduleId: Long, date: LocalDate): Boolean {
        return repository.checkDuplicateAttendance(subjectId, scheduleId, date)
    }
}
