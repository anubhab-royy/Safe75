package com.attendance.tracker.domain.model

/**
 * Severity of an integrity issue discovered in the local database.
 */
enum class IntegritySeverity {
    /** Data loss or broken references; should be repaired. */
    CRITICAL,

    /** Non-fatal anomaly worth reviewing. */
    WARNING,

    /** Informational only. */
    INFO
}

/**
 * Category grouping for [IntegrityIssue]s so the UI can present a consistent
 * taxonomy and the verifier can group related findings.
 */
enum class IntegrityCategory {
    /** Primary keys or semantically-unique rows appear more than once. */
    DUPLICATE_IDS,

    /** Rows reference subjects that no longer exist. */
    MISSING_SUBJECTS,

    /** Rows reference schedules that no longer exist. */
    MISSING_SCHEDULES,

    /** Rows reference semester versions that no longer exist. */
    MISSING_SEMESTER_VERSIONS,

    /** Attendance records whose subject or schedule reference is broken. */
    ORPHAN_ATTENDANCE,

    /** General foreign-key violation. */
    FOREIGN_KEY
}

/**
 * A single data-integrity finding with a human-readable [repairSuggestion].
 */
data class IntegrityIssue(
    val category: IntegrityCategory,
    val severity: IntegritySeverity,
    val message: String,
    val affectedCount: Int,
    val repairSuggestion: String
)

/**
 * Immutable report produced by a data-integrity scan.
 *
 * @property issues   All findings; empty when the database is healthy.
 * @property checkedAt Epoch millis when the scan completed.
 */
data class IntegrityReport(
    val issues: List<IntegrityIssue>,
    val checkedAt: Long = System.currentTimeMillis()
) {
    val isHealthy: Boolean get() = issues.isEmpty()
    val criticalCount: Int get() = issues.count { it.severity == IntegritySeverity.CRITICAL }
    val warningCount: Int get() = issues.count { it.severity == IntegritySeverity.WARNING }
    val infoCount: Int get() = issues.count { it.severity == IntegritySeverity.INFO }
}
