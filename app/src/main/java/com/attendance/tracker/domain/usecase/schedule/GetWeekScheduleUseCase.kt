package com.attendance.tracker.domain.usecase.schedule

import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.domain.repository.ScheduleRepository
import javax.inject.Inject

/**
 * Domain Use Case to retrieve the entire weekly schedule list for a timetable version.
 */
class GetWeekScheduleUseCase @Inject constructor(
    private val repository: ScheduleRepository
) {
    suspend operator fun invoke(versionId: Long): List<Schedule> {
        return repository.getSchedulesForVersion(versionId)
    }
}
