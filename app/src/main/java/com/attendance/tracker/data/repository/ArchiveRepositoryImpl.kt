package com.attendance.tracker.data.repository

import android.net.Uri
import com.attendance.tracker.core.backup.BackupAttendanceDto
import com.attendance.tracker.core.backup.BackupDeserializer
import com.attendance.tracker.core.backup.BackupScheduleDto
import com.attendance.tracker.core.backup.BackupSerializer
import com.attendance.tracker.core.backup.BackupSubjectDto
import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.data.local.database.dao.ArchiveDao
import com.attendance.tracker.data.local.database.dao.AttendanceDao
import com.attendance.tracker.data.local.database.dao.ScheduleDao
import com.attendance.tracker.data.local.database.dao.SemesterDao
import com.attendance.tracker.data.local.database.dao.SubjectDao
import com.attendance.tracker.data.local.database.entity.ArchiveEntity
import com.attendance.tracker.data.local.database.entity.AttendanceEntity
import com.attendance.tracker.data.local.database.entity.ScheduleEntity
import com.attendance.tracker.data.local.database.entity.SubjectEntity
import com.attendance.tracker.domain.model.ArchiveData
import com.attendance.tracker.domain.model.ArchiveSubjectStat
import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.model.ResetOptions
import com.attendance.tracker.domain.model.ResetPreview
import com.attendance.tracker.domain.model.ResetResult
import com.attendance.tracker.domain.model.RestoreOptions
import com.attendance.tracker.domain.model.RestoreResult
import com.attendance.tracker.core.common.DispatcherProvider
import com.attendance.tracker.domain.repository.ArchiveRepository
import com.attendance.tracker.domain.usecase.attendance.CalculateAttendanceStatisticsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [ArchiveRepository] handling semester archiving, restoration, and reset.
 *
 * Archives are stored as self-contained JSON snapshots inside [ArchiveEntity] so they remain
 * valid even after a semester reset clears the live database tables.
 */
@Singleton
class ArchiveRepositoryImpl @Inject constructor(
    private val archiveDao: ArchiveDao,
    private val subjectDao: SubjectDao,
    private val scheduleDao: ScheduleDao,
    private val semesterDao: SemesterDao,
    private val attendanceDao: AttendanceDao,
    private val serializer: BackupSerializer,
    private val deserializer: BackupDeserializer,
    private val calculateStatistics: CalculateAttendanceStatisticsUseCase,
    private val dispatcherProvider: DispatcherProvider
) : ArchiveRepository {

    // -------------------------------------------------------------------------
    // Observe
    // -------------------------------------------------------------------------

    override fun observeArchives(): Flow<List<ArchiveData>> {
        return archiveDao.observeArchives().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // -------------------------------------------------------------------------
    // Create Archive
    // -------------------------------------------------------------------------

    override suspend fun createArchive(
        name: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Long = withContext(dispatcherProvider.io) {
        val subjects = subjectDao.getSubjects().map { e ->
            BackupSubjectDto(e.id, e.name, e.facultyName, e.color,
                e.requiredAttendancePercentage, e.personalAttendanceGoal, e.createdAt, e.updatedAt)
        }
        val schedules = scheduleDao.getAllSchedules().map { e ->
            BackupScheduleDto(e.id, e.subjectId, e.dayOfWeek.name, e.startTime.toString(),
                e.endTime.toString(), e.room, e.teacherOverride, e.versionId, e.createdAt, e.updatedAt)
        }
        val attendance = attendanceDao.getAllAttendance().map { e ->
            BackupAttendanceDto(e.id, e.subjectId, e.scheduleId, e.date.toString(),
                e.status.name, e.remarks, e.createdAt, e.updatedAt)
        }

        val stats = calculateArchiveStatistics(attendance)

        val entity = ArchiveEntity(
            name = name,
            startDate = startDate.toString(),
            endDate = endDate.toString(),
            subjectsJson = serializer.serializeSubjects(subjects),
            schedulesJson = serializer.serializeSchedules(schedules),
            attendanceJson = serializer.serializeAttendance(attendance),
            totalClasses = stats.totalClasses,
            presentCount = stats.presentCount,
            absentCount = stats.absentCount,
            cancelledCount = stats.cancelledCount,
            overallPercentage = archivePercentage(stats.attendancePercentage)
        )
        archiveDao.insertArchive(entity)
    }

    // -------------------------------------------------------------------------
    // Get Archive
    // -------------------------------------------------------------------------

    override suspend fun getArchive(id: Long): ArchiveData? = withContext(dispatcherProvider.io) {
        archiveDao.getArchiveById(id)?.toDomainWithSubjectStats()
    }

    // -------------------------------------------------------------------------
    // Delete Archive
    // -------------------------------------------------------------------------

    override suspend fun deleteArchive(id: Long) = withContext(dispatcherProvider.io) {
        archiveDao.deleteArchiveById(id)
        Unit
    }

    // -------------------------------------------------------------------------
    // Restore Archive
    // -------------------------------------------------------------------------

    override suspend fun restoreArchive(id: Long, options: RestoreOptions): RestoreResult =
        withContext(dispatcherProvider.io) {
            val entity = archiveDao.getArchiveById(id)
                ?: return@withContext RestoreResult.Failure("Archive not found.")
            try {
                var subjectsRestored = 0
                var schedulesRestored = 0
                var attendanceRestored = 0

                if (options.restoreSubjects) {
                    deserializer.deserializeSubjects(entity.subjectsJson).forEach { dto ->
                        subjectDao.upsertSubject(
                            SubjectEntity(dto.id, dto.name, dto.facultyName, dto.color,
                                dto.requiredAttendancePercentage, dto.personalAttendanceGoal,
                                dto.createdAt, dto.updatedAt)
                        )
                        subjectsRestored++
                    }
                }

                if (options.restoreSchedules) {
                    deserializer.deserializeSchedules(entity.schedulesJson).forEach { dto ->
                        val day = runCatching { WeekDay.valueOf(dto.dayOfWeek) }.getOrNull()
                            ?: return@forEach
                        val start = runCatching { java.time.LocalTime.parse(dto.startTime) }.getOrNull()
                            ?: return@forEach
                        val end = runCatching { java.time.LocalTime.parse(dto.endTime) }.getOrNull()
                            ?: return@forEach
                        scheduleDao.upsertSchedule(
                            ScheduleEntity(dto.id, dto.subjectId, day, start, end,
                                dto.room, dto.teacherOverride, dto.versionId, dto.createdAt, dto.updatedAt)
                        )
                        schedulesRestored++
                    }
                }

                if (options.restoreAttendance) {
                    deserializer.deserializeAttendance(entity.attendanceJson).forEach { dto ->
                        val date = runCatching { LocalDate.parse(dto.date) }.getOrNull()
                            ?: return@forEach
                        val status = runCatching { AttendanceStatus.valueOf(dto.status) }.getOrNull()
                            ?: return@forEach
                        attendanceDao.upsertAttendance(
                            AttendanceEntity(dto.id, dto.subjectId, dto.scheduleId, date,
                                status, dto.remarks, dto.createdAt, dto.updatedAt)
                        )
                        attendanceRestored++
                    }
                }

                RestoreResult.Success(subjectsRestored, schedulesRestored, attendanceRestored)
            } catch (e: Exception) {
                RestoreResult.Failure(e.message ?: "Restore failed.")
            }
        }

    // -------------------------------------------------------------------------
    // Reset Semester
    // -------------------------------------------------------------------------

    override suspend fun resetSemester(options: ResetOptions): ResetResult =
        withContext(dispatcherProvider.io) {
            try {
                var attendanceDeleted = 0
                var subjectsDeleted = 0
                var schedulesDeleted = 0

                if (options.deleteAttendance) {
                    attendanceDeleted = attendanceDao.deleteAllAttendance()
                }

                if (!options.keepSchedules) {
                    schedulesDeleted = scheduleDao.deleteAllSchedules()
                }

                if (!options.keepSubjects) {
                    // Subjects must be deleted after schedules (FK constraint is CASCADE,
                    // but we delete explicitly to get the count)
                    subjectsDeleted = subjectDao.deleteAllSubjects()
                }

                ResetResult.Success(attendanceDeleted, subjectsDeleted, schedulesDeleted)
            } catch (e: Exception) {
                ResetResult.Failure(e.message ?: "Reset failed.")
            }
        }

    // -------------------------------------------------------------------------
    // Reset Preview
    // -------------------------------------------------------------------------

    override suspend fun getResetPreview(): ResetPreview = withContext(dispatcherProvider.io) {
        ResetPreview(
            subjectCount = subjectDao.getSubjects().size,
            scheduleCount = scheduleDao.getAllSchedules().size,
            attendanceCount = attendanceDao.getAllAttendance().size,
            semesterVersionCount = semesterDao.getVersions().size
        )
    }

    // -------------------------------------------------------------------------
    // Mapping Helpers
    // -------------------------------------------------------------------------

    private fun ArchiveEntity.toDomain(): ArchiveData {
        val attendance = deserializer.deserializeAttendance(attendanceJson)
        val stats = calculateArchiveStatistics(attendance)
        return ArchiveData(
            id = id,
            name = name,
            startDate = LocalDate.parse(startDate),
            endDate = LocalDate.parse(endDate),
            archivedAt = archivedAt,
            totalClasses = stats.totalClasses,
            presentCount = stats.presentCount,
            absentCount = stats.absentCount,
            cancelledCount = stats.cancelledCount,
            overallPercentage = archivePercentage(stats.attendancePercentage),
            medicalLeaveCount = stats.medicalLeaveCount,
            withMedicalPercentage = archivePercentage(stats.withMedicalPercentage),
            subjectStats = emptyList(),
            attendanceCount = attendance.size
        )
    }

    private fun ArchiveEntity.toDomainWithSubjectStats(): ArchiveData {
        val subjects = deserializer.deserializeSubjects(subjectsJson)
        val attendance = deserializer.deserializeAttendance(attendanceJson)
        val schedules = deserializer.deserializeSchedules(schedulesJson)

        val stats = subjects.map { subject ->
            val subjectAttendance = attendance.filter { it.subjectId == subject.id }
            val subjectStats = calculateArchiveStatistics(subjectAttendance)
            ArchiveSubjectStat(
                subjectId = subject.id,
                subjectName = subject.name,
                color = subject.color,
                totalClasses = subjectStats.totalClasses,
                presentCount = subjectStats.presentCount,
                absentCount = subjectStats.absentCount,
                cancelledCount = subjectStats.cancelledCount,
                attendancePercentage = archivePercentage(subjectStats.attendancePercentage),
                medicalLeaveCount = subjectStats.medicalLeaveCount,
                withMedicalPercentage = archivePercentage(subjectStats.withMedicalPercentage)
            )
        }

        val overallStats = calculateArchiveStatistics(attendance)

        return ArchiveData(
            id = id,
            name = name,
            startDate = LocalDate.parse(startDate),
            endDate = LocalDate.parse(endDate),
            archivedAt = archivedAt,
            totalClasses = overallStats.totalClasses,
            presentCount = overallStats.presentCount,
            absentCount = overallStats.absentCount,
            cancelledCount = overallStats.cancelledCount,
            overallPercentage = archivePercentage(overallStats.attendancePercentage),
            medicalLeaveCount = overallStats.medicalLeaveCount,
            withMedicalPercentage = archivePercentage(overallStats.withMedicalPercentage),
            subjectStats = stats,
            scheduleCount = schedules.size,
            attendanceCount = attendance.size
        )
    }

    private fun calculateArchiveStatistics(records: List<BackupAttendanceDto>) =
        calculateStatistics(records.mapNotNull { it.toDomainOrNull() }, 75, 85)

    private fun BackupAttendanceDto.toDomainOrNull(): Attendance? = runCatching {
        Attendance(
            id = id,
            subjectId = subjectId,
            scheduleId = scheduleId,
            date = LocalDate.parse(date),
            status = AttendanceStatus.valueOf(status),
            remarks = remarks,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }.getOrNull()

    private fun archivePercentage(value: Double): Double = if (value < 0.0) 0.0 else value
}
