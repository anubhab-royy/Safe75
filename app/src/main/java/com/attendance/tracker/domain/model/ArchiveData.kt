package com.attendance.tracker.domain.model

import java.time.LocalDate

/**
 * Per-subject attendance statistics inside an archived semester.
 */
data class ArchiveSubjectStat(
    val subjectId: Long,
    val subjectName: String,
    val color: Int,
    val totalClasses: Int,
    val presentCount: Int,
    val absentCount: Int,
    val cancelledCount: Int,
    val attendancePercentage: Double,
    val medicalLeaveCount: Int = 0,
    val withMedicalPercentage: Double = 0.0
)

/**
 * Read-only domain model representing a fully archived semester.
 *
 * All data is self-contained — it remains valid even after a semester reset
 * has cleared the live database tables.
 */
data class ArchiveData(
    val id: Long,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val archivedAt: Long,
    val totalClasses: Int,
    val presentCount: Int,
    val absentCount: Int,
    val cancelledCount: Int,
    val overallPercentage: Double,
    val medicalLeaveCount: Int = 0,
    val withMedicalPercentage: Double = 0.0,
    /** Per-subject breakdown; populated on-demand when opening archive details. */
    val subjectStats: List<ArchiveSubjectStat> = emptyList(),
    /** Count of schedule slots captured in the snapshot. */
    val scheduleCount: Int = 0,
    /** Count of attendance records captured in the snapshot. */
    val attendanceCount: Int = 0
)
