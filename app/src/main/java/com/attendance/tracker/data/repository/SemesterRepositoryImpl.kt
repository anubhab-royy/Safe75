package com.attendance.tracker.data.repository

import com.attendance.tracker.data.local.datasource.SemesterLocalDataSource
import com.attendance.tracker.data.mapper.SemesterMapper
import com.attendance.tracker.domain.model.Semester
import com.attendance.tracker.domain.repository.SemesterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of [SemesterRepository] delegating to [SemesterLocalDataSource].
 */
class SemesterRepositoryImpl @Inject constructor(
    private val localDataSource: SemesterLocalDataSource
) : SemesterRepository {

    override fun getSemesters(): Flow<List<Semester>> {
        return localDataSource.getAllSemesters().map { list ->
            list.map { SemesterMapper.entityToDomain(it) }
        }
    }

    override suspend fun insertSemester(semester: Semester): Long {
        return localDataSource.insertSemester(SemesterMapper.domainToEntity(semester))
    }

    override suspend fun deleteSemester(semester: Semester) {
        localDataSource.deleteSemester(SemesterMapper.domainToEntity(semester))
    }
}
