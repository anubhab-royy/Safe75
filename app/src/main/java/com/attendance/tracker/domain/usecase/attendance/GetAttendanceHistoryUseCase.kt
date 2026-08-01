package com.attendance.tracker.domain.usecase.attendance

import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Domain Use Case to observe all historical attendance logs.
 */
class GetAttendanceHistoryUseCase @Inject constructor(
    private val repository: AttendanceRepository
) {
    operator fun invoke(): Flow<List<Attendance>> {
        return repository.observeAttendanceHistory()
    }
}
