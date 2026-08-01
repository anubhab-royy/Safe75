package com.attendance.tracker.domain.usecase.attendance

import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Domain Use Case to observe attendance logs registered for a specific subject.
 */
class GetAttendanceBySubjectUseCase @Inject constructor(
    private val repository: AttendanceRepository
) {
    operator fun invoke(subjectId: Long): Flow<List<Attendance>> {
        return repository.observeAttendanceForSubject(subjectId)
    }
}
