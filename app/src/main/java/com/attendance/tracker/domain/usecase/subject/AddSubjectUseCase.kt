package com.attendance.tracker.domain.usecase.subject

import com.attendance.tracker.domain.model.Subject
import com.attendance.tracker.domain.repository.SubjectRepository
import javax.inject.Inject

/**
 * Domain Use Case to create or update a study subject.
 */
class AddSubjectUseCase @Inject constructor(
    private val repository: SubjectRepository
) {
    /**
     * Executes the insert subject transaction.
     */
    suspend operator fun invoke(subject: Subject): Long = repository.insertSubject(subject)
}
