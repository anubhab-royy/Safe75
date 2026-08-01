package com.attendance.tracker.domain.usecase.subject

import com.attendance.tracker.domain.model.Subject
import com.attendance.tracker.domain.repository.SubjectRepository
import javax.inject.Inject

/**
 * Domain Use Case to search subjects matching a text query.
 */
class SearchSubjectsUseCase @Inject constructor(
    private val repository: SubjectRepository
) {
    /**
     * Executes the subject search query.
     */
    suspend operator fun invoke(query: String): List<Subject> {
        if (query.isBlank()) return repository.getSubjects()
        return repository.searchSubjects(query.trim())
    }
}
