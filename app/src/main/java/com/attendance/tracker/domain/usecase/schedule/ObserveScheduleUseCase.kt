package com.attendance.tracker.domain.usecase.schedule

import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Domain Use Case to observe a live flow of all scheduled classes for a specific version.
 */
class ObserveScheduleUseCase @Inject constructor(
    private val repository: ScheduleRepository
) {
    operator fun invoke(versionId: Long): Flow<List<Schedule>> {
        return repository.observeSchedulesForVersion(versionId)
    }
}
