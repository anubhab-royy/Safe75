package com.attendance.tracker.domain.usecase.attendance

import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.repository.AttendanceRepository
import javax.inject.Inject

/**
 * Domain Use Case to delete an attendance record.
 */
class DeleteAttendanceUseCase @Inject constructor(
    private val repository: AttendanceRepository
) {
    suspend operator fun invoke(attendance: Attendance): Int {
        return repository.deleteAttendance(attendance)
    }
}
