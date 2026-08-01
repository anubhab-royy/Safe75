package com.attendance.tracker.domain.usecase.subject

import com.attendance.tracker.domain.model.Subject
import com.attendance.tracker.domain.repository.SubjectRepository
import javax.inject.Inject

/**
 * Domain Use Case to modify an existing study subject.
 */
class UpdateSubjectUseCase @Inject constructor(
    private val repository: SubjectRepository
) {
    /**
     * Executes the update subject transaction.
     */
    suspend operator fun invoke(subject: Subject): Long = repository.insertSubject(subject)
}
