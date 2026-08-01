package com.attendance.tracker.data.repository

import com.attendance.tracker.data.local.datasource.AttendanceLocalDataSource
import com.attendance.tracker.data.mapper.AttendanceMapper
import com.attendance.tracker.domain.model.Attendance
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

    override fun observeAttendanceForSubject(subjectId: Long): Flow<List<Attendance>> {
        return localDataSource.observeAttendanceForSubject(subjectId).map { list ->
            list.map { AttendanceMapper.entityToDomain(it) }
        }
    }

    override fun observeAttendanceForDate(date: LocalDate): Flow<List<Attendance>> {
        return localDataSource.observeAttendanceForDate(date).map { list ->
            list.map { AttendanceMapper.entityToDomain(it) }
        }
    }

    override fun observeTodayAttendance(date: LocalDate): Flow<List<Attendance>> {
        return localDataSource.observeTodayAttendance(date).map { list ->
            list.map { AttendanceMapper.entityToDomain(it) }
        }
    }

    override fun observeAttendanceHistory(): Flow<List<Attendance>> {
        return localDataSource.observeAttendanceHistory().map { list ->
            list.map { AttendanceMapper.entityToDomain(it) }
        }
    }

    override fun observeAttendanceById(id: Long): Flow<Attendance?> {
        return localDataSource.observeAttendanceById(id).map { entity ->
            entity?.let { AttendanceMapper.entityToDomain(it) }
        }
    }

    override suspend fun getAttendanceById(id: Long): Attendance? {
        return localDataSource.getAttendanceById(id)?.let { AttendanceMapper.entityToDomain(it) }
    }

    override suspend fun getAttendanceForDateSync(date: LocalDate): List<Attendance> {
        return localDataSource.getAttendanceForDateSync(date).map { AttendanceMapper.entityToDomain(it) }
    }

    override suspend fun insertAttendance(attendance: Attendance): Long {
        return localDataSource.insertAttendance(AttendanceMapper.domainToEntity(attendance))
    }

    override suspend fun updateAttendance(attendance: Attendance): Int {
        return localDataSource.updateAttendance(AttendanceMapper.domainToEntity(attendance))
    }

    override suspend fun deleteAttendance(attendance: Attendance): Int {
        return localDataSource.deleteAttendance(AttendanceMapper.domainToEntity(attendance))
    }

    override suspend fun checkDuplicateAttendance(subjectId: Long, scheduleId: Long, date: LocalDate): Boolean {
        return localDataSource.checkDuplicateAttendance(subjectId, scheduleId, date)
    }

    override fun countPresent(): Flow<Int> {
        return localDataSource.countPresent()
    }

    override fun countAbsent(): Flow<Int> {
        return localDataSource.countAbsent()
    }

    override fun countCancelled(): Flow<Int> {
        return localDataSource.countCancelled()
    }

    override fun searchAttendance(query: String): Flow<List<Attendance>> {
        return localDataSource.searchAttendance(query).map { list ->
            list.map { AttendanceMapper.entityToDomain(it) }
        }
    }
}
