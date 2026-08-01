package com.attendance.tracker.domain.usecase.attendance

import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/**
 * Domain Use Case to observe attendance logs recorded on a specific date.
 */
class GetAttendanceByDateUseCase @Inject constructor(
    private val repository: AttendanceRepository
) {
    operator fun invoke(date: LocalDate): Flow<List<Attendance>> {
        return repository.observeAttendanceForDate(date)
    }
}
