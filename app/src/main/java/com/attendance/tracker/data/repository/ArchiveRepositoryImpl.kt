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
import com.attendance.tracker.domain.model.ResetOptions
import com.attendance.tracker.domain.model.ResetPreview
import com.attendance.tracker.domain.model.ResetResult
import com.attendance.tracker.domain.model.RestoreOptions
import com.attendance.tracker.domain.model.RestoreResult
import com.attendance.tracker.domain.repository.ArchiveRepository
import kotlinx.coroutines.Dispatchers
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
    private val deserializer: BackupDeserializer
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
    ): Long = withContext(Dispatchers.IO) {
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

        // Compute stats (excluding CANCELLED from denominator)
        val totalClasses = attendance.size
        val present = attendance.count { it.status == "PRESENT" }
        val absent = attendance.count { it.status == "ABSENT" }
        val cancelled = attendance.count { it.status == "CANCELLED" }
        val denominator = totalClasses - cancelled
        val overallPct = if (denominator > 0) (present.toDouble() / denominator) * 100.0 else 0.0

        val entity = ArchiveEntity(
            name = name,
            startDate = startDate.toString(),
            endDate = endDate.toString(),
            subjectsJson = serializer.serializeSubjects(subjects),
            schedulesJson = serializer.serializeSchedules(schedules),
            attendanceJson = serializer.serializeAttendance(attendance),
            totalClasses = totalClasses,
            presentCount = present,
            absentCount = absent,
            cancelledCount = cancelled,
            overallPercentage = overallPct
        )
        archiveDao.insertArchive(entity)
    }

    // -------------------------------------------------------------------------
    // Get Archive
    // -------------------------------------------------------------------------

    override suspend fun getArchive(id: Long): ArchiveData? = withContext(Dispatchers.IO) {
        archiveDao.getArchiveById(id)?.toDomainWithSubjectStats()
    }

    // -------------------------------------------------------------------------
    // Delete Archive
    // -------------------------------------------------------------------------

    override suspend fun deleteArchive(id: Long) = withContext(Dispatchers.IO) {
        archiveDao.deleteArchiveById(id)
        Unit
    }

    // -------------------------------------------------------------------------
    // Restore Archive
    // -------------------------------------------------------------------------

    override suspend fun restoreArchive(id: Long, options: RestoreOptions): RestoreResult =
        withContext(Dispatchers.IO) {
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
        withContext(Dispatchers.IO) {
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

    override suspend fun getResetPreview(): ResetPreview = withContext(Dispatchers.IO) {
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

    private fun ArchiveEntity.toDomain(): ArchiveData = ArchiveData(
        id = id,
        name = name,
        startDate = LocalDate.parse(startDate),
        endDate = LocalDate.parse(endDate),
        archivedAt = archivedAt,
        totalClasses = totalClasses,
        presentCount = presentCount,
        absentCount = absentCount,
        cancelledCount = cancelledCount,
        overallPercentage = overallPercentage,
        subjectStats = emptyList()
    )

    private fun ArchiveEntity.toDomainWithSubjectStats(): ArchiveData {
        val subjects = deserializer.deserializeSubjects(subjectsJson)
        val attendance = deserializer.deserializeAttendance(attendanceJson)

        val stats = subjects.map { subject ->
            val subjectAttendance = attendance.filter { it.subjectId == subject.id }
            val total = subjectAttendance.size
            val present = subjectAttendance.count { it.status == "PRESENT" }
            val absent = subjectAttendance.count { it.status == "ABSENT" }
            val cancelled = subjectAttendance.count { it.status == "CANCELLED" }
            val denom = total - cancelled
            val pct = if (denom > 0) (present.toDouble() / denom) * 100.0 else 0.0
            ArchiveSubjectStat(
                subjectId = subject.id,
                subjectName = subject.name,
                color = subject.color,
                totalClasses = total,
                presentCount = present,
                absentCount = absent,
                cancelledCount = cancelled,
                attendancePercentage = pct
            )
        }

        return ArchiveData(
            id = id,
            name = name,
            startDate = LocalDate.parse(startDate),
            endDate = LocalDate.parse(endDate),
            archivedAt = archivedAt,
            totalClasses = totalClasses,
            presentCount = presentCount,
            absentCount = absentCount,
            cancelledCount = cancelledCount,
            overallPercentage = overallPercentage,
            subjectStats = stats
        )
    }
}
