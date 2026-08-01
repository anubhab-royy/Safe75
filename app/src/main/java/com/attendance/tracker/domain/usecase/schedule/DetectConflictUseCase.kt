package com.attendance.tracker.domain.usecase.schedule

import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.domain.repository.ScheduleRepository
import java.time.LocalTime
import javax.inject.Inject

/**
 * Domain Use Case to scan and check timing conflicts for class scheduling.
 */
class DetectConflictUseCase @Inject constructor(
    private val repository: ScheduleRepository
) {
    /**
     * Finds conflicting schedules in the database, excluding a specific schedule ID during edits.
     */
    suspend operator fun invoke(
        versionId: Long,
        day: WeekDay,
        start: LocalTime,
        end: LocalTime,
        excludeScheduleId: Long = 0L
    ): List<Schedule> {
        return repository.checkConflicts(versionId, day, start, end)
            .filter { it.id != excludeScheduleId }
    }
}
