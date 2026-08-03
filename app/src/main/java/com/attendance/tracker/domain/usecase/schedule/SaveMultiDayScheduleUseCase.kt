package com.attendance.tracker.domain.usecase.schedule

import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.domain.repository.ScheduleRepository
import java.time.LocalTime
import javax.inject.Inject

/**
 * Domain Use Case that persists one schedule record per selected weekday.
 *
 * When creating a new slot ([anchorId] == 0L) a fresh record is inserted for
 * every selected day. When editing an existing slot the provided schedule acts
 * as the anchor: it is updated to represent the first selected day, other
 * selected days are updated/inserted as needed, and any sibling records that
 * belong to the same subject/time group but are no longer selected are removed.
 *
 * This keeps the existing single-day [Schedule] model intact while letting the
 * UI express multi-day class slots.
 */
class SaveMultiDayScheduleUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) {

    suspend operator fun invoke(
        versionId: Long,
        anchorId: Long,
        subjectId: Long,
        days: Set<WeekDay>,
        startTime: LocalTime,
        endTime: LocalTime,
        room: String?,
        teacher: String?
    ) {
        val sortedDays = WeekDay.ordered.filter { it in days }
        require(sortedDays.isNotEmpty()) { "At least one weekday must be selected" }
        val trimmedRoom = room?.trim()?.takeIf { it.isNotBlank() }
        val trimmedTeacher = teacher?.trim()?.takeIf { it.isNotBlank() }
        val now = System.currentTimeMillis()

        val group = scheduleRepository.getSchedulesForVersion(versionId).filter {
            it.subjectId == subjectId && it.startTime == startTime && it.endTime == endTime
        }
        val groupByDay = group.associateBy { it.dayOfWeek }
        val anchor = group.find { it.id == anchorId }

        if (anchor != null) {
            val anchorFinalDay = if (anchor.dayOfWeek in days) anchor.dayOfWeek else sortedDays.first()

            // If the anchor is moved onto a day already occupied by a sibling, remove that sibling.
            val occupant = groupByDay[anchorFinalDay]
            if (occupant != null && occupant.id != anchorId) {
                scheduleRepository.deleteSchedule(occupant)
            }

            scheduleRepository.updateSchedule(
                anchor.copy(
                    dayOfWeek = anchorFinalDay,
                    room = trimmedRoom,
                    teacherOverride = trimmedTeacher,
                    updatedAt = now
                )
            )

            for (day in sortedDays) {
                if (day == anchorFinalDay) continue
                val existingForDay = groupByDay[day]
                if (existingForDay != null && existingForDay.id != anchorId) {
                    scheduleRepository.updateSchedule(
                        existingForDay.copy(
                            room = trimmedRoom,
                            teacherOverride = trimmedTeacher,
                            updatedAt = now
                        )
                    )
                } else if (existingForDay == null) {
                    scheduleRepository.insertSchedule(
                        newSchedule(
                            versionId, subjectId, day, startTime, endTime,
                            trimmedRoom, trimmedTeacher, now
                        )
                    )
                }
            }

            // Remove siblings whose days are no longer part of the selection.
            for (member in group) {
                if (member.id != anchorId && member.dayOfWeek !in days) {
                    scheduleRepository.deleteSchedule(member)
                }
            }
        } else {
            // New slot (or the anchor was deleted externally): ensure one record per day.
            for (day in sortedDays) {
                val existingForDay = groupByDay[day]
                if (existingForDay != null) {
                    scheduleRepository.updateSchedule(
                        existingForDay.copy(
                            room = trimmedRoom,
                            teacherOverride = trimmedTeacher,
                            updatedAt = now
                        )
                    )
                } else {
                    scheduleRepository.insertSchedule(
                        newSchedule(
                            versionId, subjectId, day, startTime, endTime,
                            trimmedRoom, trimmedTeacher, now
                        )
                    )
                }
            }
        }
    }

    private fun newSchedule(
        versionId: Long,
        subjectId: Long,
        day: WeekDay,
        startTime: LocalTime,
        endTime: LocalTime,
        room: String?,
        teacher: String?,
        now: Long
    ): Schedule {
        return Schedule(
            subjectId = subjectId,
            dayOfWeek = day,
            startTime = startTime,
            endTime = endTime,
            room = room,
            teacherOverride = teacher,
            versionId = versionId,
            createdAt = now,
            updatedAt = now
        )
    }
}
