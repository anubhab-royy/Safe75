package com.attendance.tracker.feature.subject.model

/**
 * UI-layer representation of a study subject, decoupled from database annotations.
 */
data class SubjectUiModel(
    val id: Long,
    val name: String,
    val faculty: String? = null,
    val requiredAttendance: Int = 75,
    val attendanceGoal: Int = 85,
    val color: Int = 0
)

/**
 * UI-layer representation of a study subject along with its computed attendance stats.
 */
data class SubjectWithStats(
    val id: Long,
    val name: String,
    val faculty: String? = null,
    val requiredAttendance: Int = 75,
    val attendanceGoal: Int = 85,
    val color: Int = 0,
    val presentCount: Int = 0,
    val totalClasses: Int = 0,
    val percentage: Double = 0.0,
    val withMedicalPercentage: Double = -1.0,
    val safeMissCount: Int = 0,
    val classesNeeded: Int = 0,
    val safetyStatus: String = "GOOD", // "GOOD", "WARNING", "CRITICAL"
    val hasSchedules: Boolean = false,
    val trend: String = "●" // "▲" (Improving), "▼" (Dropping), "●" (Stable)
)
