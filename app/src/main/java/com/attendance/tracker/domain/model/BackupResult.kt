package com.attendance.tracker.domain.model

/** Result of a backup export or import operation. */
sealed class BackupResult {
    /** Export/import completed successfully. */
    data class Success(
        val filePath: String = "",
        val subjectsCount: Int = 0,
        val schedulesCount: Int = 0,
        val attendanceCount: Int = 0
    ) : BackupResult()

    /** Operation failed with an error message. */
    data class Failure(val error: String) : BackupResult()
}

/** Result of a selective restore operation. */
sealed class RestoreResult {
    data class Success(
        val subjectsRestored: Int = 0,
        val schedulesRestored: Int = 0,
        val attendanceRestored: Int = 0,
        val semesterVersionsRestored: Int = 0
    ) : RestoreResult()

    data class Failure(val error: String) : RestoreResult()
}

/** Result of a semester reset operation. */
sealed class ResetResult {
    data class Success(
        val attendanceDeleted: Int = 0,
        val subjectsDeleted: Int = 0,
        val schedulesDeleted: Int = 0
    ) : ResetResult()

    data class Failure(val error: String) : ResetResult()
}
