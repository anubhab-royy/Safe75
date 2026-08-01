package com.attendance.tracker.domain.repository

import com.attendance.tracker.domain.model.ArchiveData
import com.attendance.tracker.domain.model.ResetOptions
import com.attendance.tracker.domain.model.ResetPreview
import com.attendance.tracker.domain.model.ResetResult
import com.attendance.tracker.domain.model.RestoreOptions
import com.attendance.tracker.domain.model.RestoreResult
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository interface for semester archive and reset lifecycle management.
 */
interface ArchiveRepository {
    /**
     * Observes all archived semesters, ordered by most recent first.
     */
    fun observeArchives(): Flow<List<ArchiveData>>

    /**
     * Creates a new archive snapshot from the current live database state.
     *
     * @param name       User-defined name for the semester.
     * @param startDate  Start of the archived semester period.
     * @param endDate    End of the archived semester period.
     * @return The ID of the newly inserted [ArchiveData].
     */
    suspend fun createArchive(name: String, startDate: LocalDate, endDate: LocalDate): Long

    /**
     * Returns a single archive by its ID, or null if it does not exist.
     */
    suspend fun getArchive(id: Long): ArchiveData?

    /**
     * Permanently deletes an archived semester.
     */
    suspend fun deleteArchive(id: Long)

    /**
     * Restores subjects, schedules, and attendance from an archived semester
     * into the live database according to [options].
     */
    suspend fun restoreArchive(id: Long, options: RestoreOptions): RestoreResult

    /**
     * Executes a semester reset based on [options]:
     * - Optionally deletes attendance records.
     * - Optionally deletes schedules.
     * - Optionally deletes subjects.
     */
    suspend fun resetSemester(options: ResetOptions): ResetResult

    /**
     * Returns current record counts so the reset wizard can preview the impact.
     */
    suspend fun getResetPreview(): ResetPreview
}
