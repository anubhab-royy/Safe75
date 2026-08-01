package com.attendance.tracker.domain.usecase.subject

import com.attendance.tracker.domain.model.Subject
import com.attendance.tracker.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Domain Use Case to observe a live Flow of all study subjects.
 */
class ObserveSubjectsUseCase @Inject constructor(
    private val repository: SubjectRepository
) {
    /**
     * Executes the observe subjects flow.
     */
    operator fun invoke(): Flow<List<Subject>> = repository.observeSubjects()
}
