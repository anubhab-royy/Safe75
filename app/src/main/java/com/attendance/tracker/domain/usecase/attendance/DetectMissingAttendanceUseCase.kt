package com.attendance.tracker.domain.usecase.attendance

import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.model.MissingAttendanceItem
import com.attendance.tracker.domain.model.Schedule
import java.time.LocalDate
import javax.inject.Inject

/**
 * Domain Use Case that detects historical class occurrences that have no
 * attendance record yet.
 *
 * Expected occurrences are generated from [from] to [to] (inclusive) using the
 * weekdays defined by each [Schedule]. An occurrence is considered missing when
 * no attendance record exists for the matching (scheduleId, date) pair.
 */
class DetectMissingAttendanceUseCase @Inject constructor() {

    operator fun invoke(
        schedules: List<Schedule>,
        attendance: List<Attendance>,
        from: LocalDate,
        to: LocalDate
    ): List<MissingAttendanceItem> {
        if (schedules.isEmpty() || from.isAfter(to)) {
            return emptyList()
        }

        val recordedKeys = attendance
            .map { it.scheduleId to it.date }
            .toHashSet()
        val schedulesByDay = schedules.groupBy { it.dayOfWeek }

        val result = mutableListOf<MissingAttendanceItem>()
        var date = from
        while (!date.isAfter(to)) {
            val weekDay = WeekDay.fromJavaDayOfWeek(date.dayOfWeek)
            schedulesByDay[weekDay]?.forEach { schedule ->
                if (schedule.id to date !in recordedKeys) {
                    result += MissingAttendanceItem(
                        scheduleId = schedule.id,
                        subjectId = schedule.subjectId,
                        date = date,
                        dayOfWeek = weekDay,
                        startTime = schedule.startTime,
                        endTime = schedule.endTime,
                        room = schedule.room,
                        teacherOverride = schedule.teacherOverride
                    )
                }
            }
            date = date.plusDays(1)
        }

        return result.sortedWith(compareBy<MissingAttendanceItem> { it.date }.thenBy { it.startTime })
    }
}
