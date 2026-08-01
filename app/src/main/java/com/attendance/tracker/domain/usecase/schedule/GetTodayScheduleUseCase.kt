package com.attendance.tracker.domain.usecase.schedule

import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.domain.repository.ScheduleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Domain Use Case to load a Flow of classes scheduled on a specific day of the week.
 */
class GetTodayScheduleUseCase @Inject constructor(
    private val repository: ScheduleRepository
) {
    operator fun invoke(versionId: Long, day: WeekDay): Flow<List<Schedule>> {
        return repository.observeTodaySchedules(versionId, day)
    }
}
