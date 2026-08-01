package com.attendance.tracker.domain.usecase.planner

import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.domain.model.AffectedSubject
import com.attendance.tracker.domain.model.PlannerResult
import com.attendance.tracker.domain.repository.AttendanceRepository
import com.attendance.tracker.domain.repository.ScheduleRepository
import com.attendance.tracker.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Domain Use Case to analyze projected leaves and calculate projected attendance rates.
 */
class LeavePlannerUseCase @Inject constructor(
    private val subjectRepository: SubjectRepository,
    private val scheduleRepository: ScheduleRepository,
    private val attendanceRepository: AttendanceRepository
) {
    suspend operator fun invoke(
        selectedDates: List<LocalDate>,
        activeVersionId: Long
    ): PlannerResult {
        val subjects = subjectRepository.getSubjects()
        val allSchedules = scheduleRepository.getSchedulesForVersion(activeVersionId)
        val history = attendanceRepository.observeAttendanceHistory().first()

        val subjectMissedCounts = mutableMapOf<Long, Int>()

        selectedDates.forEach { date ->
            val dayOfWeek = date.dayOfWeek
            val dayEnum = when (dayOfWeek) {
                java.time.DayOfWeek.MONDAY -> WeekDay.Monday
                java.time.DayOfWeek.TUESDAY -> WeekDay.Tuesday
                java.time.DayOfWeek.WEDNESDAY -> WeekDay.Wednesday
                java.time.DayOfWeek.THURSDAY -> WeekDay.Thursday
                java.time.DayOfWeek.FRIDAY -> WeekDay.Friday
                java.time.DayOfWeek.SATURDAY -> WeekDay.Saturday
                java.time.DayOfWeek.SUNDAY -> WeekDay.Sunday
            }

            val schedulesOnDay = allSchedules.filter { it.dayOfWeek == dayEnum }
            schedulesOnDay.forEach { schedule ->
                subjectMissedCounts[schedule.subjectId] = (subjectMissedCounts[schedule.subjectId] ?: 0) + 1
            }
        }

        val affected = subjects.mapNotNull { subject ->
            val subjectHistory = history.filter { it.subjectId == subject.id }
            val present = subjectHistory.count { it.status == AttendanceStatus.PRESENT }
            val absent = subjectHistory.count { it.status == AttendanceStatus.ABSENT }
            val total = present + absent

            val currentPct = if (total > 0) {
                (present.toDouble() / total.toDouble()) * 100.0
            } else {
                100.0
            }

            val missedCount = subjectMissedCounts[subject.id] ?: 0
            if (missedCount == 0) return@mapNotNull null

            val projectedAbsent = absent + missedCount
            val projectedTotal = present + projectedAbsent
            val projectedPct = if (projectedTotal > 0) {
                (present.toDouble() / projectedTotal.toDouble()) * 100.0
            } else {
                100.0
            }

            AffectedSubject(
                subjectId = subject.id,
                subjectName = subject.name,
                subjectColor = subject.color,
                currentPercentage = currentPct,
                projectedPercentage = projectedPct,
                missedClassesCount = missedCount,
                isProjectedSafe = projectedPct >= 75.0
            )
        }

        val totalPresent = history.count { it.status == AttendanceStatus.PRESENT }
        val totalAbsent = history.count { it.status == AttendanceStatus.ABSENT }
        val totalCurrent = totalPresent + totalAbsent

        val currentOverallPct = if (totalCurrent > 0) {
            (totalPresent.toDouble() / totalCurrent.toDouble()) * 100.0
        } else {
            100.0
        }

        val totalMissed = affected.sumOf { it.missedClassesCount }
        val totalProjectedAbsent = totalAbsent + totalMissed
        val totalProjected = totalPresent + totalProjectedAbsent

        val projectedOverallPct = if (totalProjected > 0) {
            (totalPresent.toDouble() / totalProjected.toDouble()) * 100.0
        } else {
            100.0
        }

        val isOverallSafe = affected.all { it.isProjectedSafe } && projectedOverallPct >= 75.0

        return PlannerResult(
            affectedSubjects = affected,
            isOverallSafe = isOverallSafe,
            currentOverallPercentage = currentOverallPct,
            projectedOverallPercentage = projectedOverallPct
        )
    }
}
