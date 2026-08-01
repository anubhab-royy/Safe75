package com.attendance.tracker.domain.usecase.schedule

import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.domain.repository.ScheduleRepository
import javax.inject.Inject

/**
 * Domain Use Case to modify an existing schedule config.
 */
class UpdateScheduleUseCase @Inject constructor(
    private val repository: ScheduleRepository
) {
    suspend operator fun invoke(schedule: Schedule): Int = repository.updateSchedule(schedule)
}
