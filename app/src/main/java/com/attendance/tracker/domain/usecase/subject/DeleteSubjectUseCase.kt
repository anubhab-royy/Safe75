package com.attendance.tracker.domain.usecase.subject

import com.attendance.tracker.domain.model.Subject
import com.attendance.tracker.domain.repository.SubjectRepository
import javax.inject.Inject

/**
 * Domain Use Case to remove a study subject.
 */
class DeleteSubjectUseCase @Inject constructor(
    private val repository: SubjectRepository
) {
    /**
     * Executes the delete subject transaction.
     */
    suspend operator fun invoke(subject: Subject) {
        repository.deleteSubject(subject)
    }
}
