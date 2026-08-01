package com.attendance.tracker.domain.usecase.subject

import com.attendance.tracker.domain.model.Subject
import com.attendance.tracker.domain.repository.SubjectRepository
import javax.inject.Inject

/**
 * Domain Use Case to fetch a single subject by ID.
 */
class GetSubjectUseCase @Inject constructor(
    private val repository: SubjectRepository
) {
    /**
     * Executes the fetch subject by ID transaction.
     */
    suspend operator fun invoke(id: Long): Subject? = repository.getSubjectById(id)
}
