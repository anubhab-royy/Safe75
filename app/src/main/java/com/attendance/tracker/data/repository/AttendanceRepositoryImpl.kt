package com.attendance.tracker.data.repository

import com.attendance.tracker.data.local.datasource.AttendanceLocalDataSource
import com.attendance.tracker.data.mapper.AttendanceMapper
import com.attendance.tracker.domain.model.AttendanceRecord
import com.attendance.tracker.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Implementation of [AttendanceRepository] delegating to [AttendanceLocalDataSource].
 */
class AttendanceRepositoryImpl @Inject constructor(
    private val localDataSource: AttendanceLocalDataSource
) : AttendanceRepository {

    override fun getAttendanceRecordsForSubject(subjectId: Long): Flow<List<AttendanceRecord>> {
        return localDataSource.getAttendanceForSubject(subjectId).map { list ->
            list.map { AttendanceMapper.entityToDomain(it) }
        }
    }

    override fun getAttendanceRecordsForDate(date: LocalDate): Flow<List<AttendanceRecord>> {
        return localDataSource.getAttendanceForDate(date).map { list ->
            list.map { AttendanceMapper.entityToDomain(it) }
        }
    }

    override suspend fun insertAttendanceRecord(record: AttendanceRecord): Long {
        return localDataSource.insertAttendance(AttendanceMapper.domainToEntity(record))
    }

    override suspend fun deleteAttendanceRecord(record: AttendanceRecord) {
        localDataSource.deleteAttendance(AttendanceMapper.domainToEntity(record))
    }
}
