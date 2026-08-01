package com.attendance.tracker.domain.usecase.schedule

import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.domain.repository.ScheduleRepository
import javax.inject.Inject

/**
 * Domain Use Case to create a new weekly class schedule entry.
 */
class AddScheduleUseCase @Inject constructor(
    private val repository: ScheduleRepository
) {
    suspend operator fun invoke(schedule: Schedule): Long = repository.insertSchedule(schedule)
}
