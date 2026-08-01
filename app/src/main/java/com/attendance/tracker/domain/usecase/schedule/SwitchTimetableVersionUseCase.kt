package com.attendance.tracker.domain.usecase.schedule

import com.attendance.tracker.domain.repository.SemesterRepository
import javax.inject.Inject

/**
 * Domain Use Case to set the active timetable version.
 */
class SwitchTimetableVersionUseCase @Inject constructor(
    private val repository: SemesterRepository
) {
    suspend operator fun invoke(versionId: Long) {
        repository.switchActiveVersion(versionId)
    }
}
