package com.attendance.tracker.data.repository

import com.attendance.tracker.data.local.datasource.SubjectLocalDataSource
import com.attendance.tracker.data.mapper.SubjectMapper
import com.attendance.tracker.domain.model.Subject
import com.attendance.tracker.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of [SubjectRepository] delegating to [SubjectLocalDataSource].
 */
class SubjectRepositoryImpl @Inject constructor(
    private val localDataSource: SubjectLocalDataSource
) : SubjectRepository {

    override fun getSubjects(): Flow<List<Subject>> {
        return localDataSource.getAllSubjects().map { list ->
            list.map { SubjectMapper.entityToDomain(it) }
        }
    }

    override suspend fun getSubjectById(id: Long): Subject? {
        return null // Skeleton placeholder, implemented in later phases.
    }

    override suspend fun insertSubject(subject: Subject): Long {
        return localDataSource.insertSubject(SubjectMapper.domainToEntity(subject))
    }

    override suspend fun deleteSubject(subject: Subject) {
        localDataSource.deleteSubject(SubjectMapper.domainToEntity(subject))
    }
}
