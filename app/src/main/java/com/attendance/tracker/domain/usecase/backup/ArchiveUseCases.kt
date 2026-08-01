package com.attendance.tracker.domain.usecase.backup

import com.attendance.tracker.domain.model.ArchiveData
import com.attendance.tracker.domain.model.ResetOptions
import com.attendance.tracker.domain.model.ResetPreview
import com.attendance.tracker.domain.model.ResetResult
import com.attendance.tracker.domain.model.RestoreOptions
import com.attendance.tracker.domain.model.RestoreResult
import com.attendance.tracker.domain.repository.ArchiveRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/**
 * Creates a self-contained archive snapshot from the current database state.
 */
class CreateArchiveUseCase @Inject constructor(
    private val archiveRepository: ArchiveRepository
) {
    suspend operator fun invoke(
        name: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Long {
        return archiveRepository.createArchive(name, startDate, endDate)
    }
}

/**
 * Observes the list of all archived semesters.
 */
class GetArchivesUseCase @Inject constructor(
    private val archiveRepository: ArchiveRepository
) {
    operator fun invoke(): Flow<List<ArchiveData>> {
        return archiveRepository.observeArchives()
    }
}

/**
 * Returns a single archive by ID, or null.
 */
class GetArchiveUseCase @Inject constructor(
    private val archiveRepository: ArchiveRepository
) {
    suspend operator fun invoke(id: Long): ArchiveData? {
        return archiveRepository.getArchive(id)
    }
}

/**
 * Restores data from an archived semester into the live database.
 */
class RestoreArchiveUseCase @Inject constructor(
    private val archiveRepository: ArchiveRepository
) {
    suspend operator fun invoke(id: Long, options: RestoreOptions): RestoreResult {
        return archiveRepository.restoreArchive(id, options)
    }
}

/**
 * Permanently deletes an archived semester from the database.
 */
class DeleteArchiveUseCase @Inject constructor(
    private val archiveRepository: ArchiveRepository
) {
    suspend operator fun invoke(id: Long) {
        archiveRepository.deleteArchive(id)
    }
}

/**
 * Executes a semester reset based on user-selected [ResetOptions].
 */
class SemesterResetUseCase @Inject constructor(
    private val archiveRepository: ArchiveRepository
) {
    suspend operator fun invoke(options: ResetOptions): ResetResult {
        return archiveRepository.resetSemester(options)
    }
}

/**
 * Returns the current record counts to preview the impact of a semester reset.
 */
class GetResetPreviewUseCase @Inject constructor(
    private val archiveRepository: ArchiveRepository
) {
    suspend operator fun invoke(): ResetPreview {
        return archiveRepository.getResetPreview()
    }
}
