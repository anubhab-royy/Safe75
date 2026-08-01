package com.attendance.tracker.domain.usecase.schedule

import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.domain.repository.ScheduleRepository
import javax.inject.Inject

/**
 * Domain Use Case to remove a weekly schedule config.
 */
class DeleteScheduleUseCase @Inject constructor(
    private val repository: ScheduleRepository
) {
    suspend operator fun invoke(schedule: Schedule): Int = repository.deleteSchedule(schedule)
}
