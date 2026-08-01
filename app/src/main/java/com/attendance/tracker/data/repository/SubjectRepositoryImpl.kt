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

    override fun observeSubjects(): Flow<List<Subject>> {
        return localDataSource.observeSubjects().map { list ->
            list.map { SubjectMapper.entityToDomain(it) }
        }
    }

    override suspend fun getSubjects(): List<Subject> {
        return localDataSource.getSubjects().map { SubjectMapper.entityToDomain(it) }
    }

    override suspend fun getSubjectById(id: Long): Subject? {
        return localDataSource.getSubject(id)?.let { SubjectMapper.entityToDomain(it) }
    }

    override suspend fun insertSubject(subject: Subject): Long {
        return localDataSource.insertSubject(SubjectMapper.domainToEntity(subject))
    }

    override suspend fun updateSubject(subject: Subject): Int {
        return localDataSource.updateSubject(SubjectMapper.domainToEntity(subject))
    }

    override suspend fun deleteSubject(subject: Subject): Int {
        return localDataSource.deleteSubject(SubjectMapper.domainToEntity(subject))
    }

    override suspend fun searchSubjects(query: String): List<Subject> {
        return localDataSource.searchSubjects(query).map { SubjectMapper.entityToDomain(it) }
    }

    override suspend fun countSubjects(): Int {
        return localDataSource.countSubjects()
    }
}
