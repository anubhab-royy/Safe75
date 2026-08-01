package com.attendance.tracker.data.repository

import android.content.Context
import android.net.Uri
import com.attendance.tracker.core.backup.BackupAttendanceDto
import com.attendance.tracker.core.backup.BackupData
import com.attendance.tracker.core.backup.BackupManager
import com.attendance.tracker.core.backup.BackupMetadata
import com.attendance.tracker.core.backup.BackupParseResult
import com.attendance.tracker.core.backup.BackupScheduleDto
import com.attendance.tracker.core.backup.BackupSemesterDto
import com.attendance.tracker.core.backup.BackupSettingsDto
import com.attendance.tracker.core.backup.BackupSubjectDto
import com.attendance.tracker.core.backup.ValidationResult
import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.data.local.database.dao.AttendanceDao
import com.attendance.tracker.data.local.database.dao.ScheduleDao
import com.attendance.tracker.data.local.database.dao.SemesterDao
import com.attendance.tracker.data.local.database.dao.SubjectDao
import com.attendance.tracker.data.local.database.entity.AttendanceEntity
import com.attendance.tracker.data.local.database.entity.ScheduleEntity
import com.attendance.tracker.data.local.database.entity.SemesterVersionEntity
import com.attendance.tracker.data.local.database.entity.SubjectEntity
import com.attendance.tracker.data.local.preferences.SettingsPreferences
import com.attendance.tracker.domain.model.BackupResult
import com.attendance.tracker.domain.model.RestoreOptions
import com.attendance.tracker.domain.model.RestoreResult
import com.attendance.tracker.domain.repository.BackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [BackupRepository] that reads from and writes to the local Room database.
 *
 * Parsing, validation, and error classification are delegated to [BackupManager] so the
 * app owns exactly one serialization pipeline. File I/O uses the Android
 * [android.content.ContentResolver] to support SAF URIs.
 */
@Singleton
class BackupRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val subjectDao: SubjectDao,
    private val scheduleDao: ScheduleDao,
    private val semesterDao: SemesterDao,
    private val attendanceDao: AttendanceDao,
    private val settingsPreferences: SettingsPreferences,
    private val backupManager: BackupManager
) : BackupRepository {

    // -------------------------------------------------------------------------
    // Export
    // -------------------------------------------------------------------------

    override suspend fun exportBackup(destinationUri: Uri): BackupResult =
        withContext(Dispatchers.IO) {
            try {
                val data = getBackupData()
                val json = backupManager.serialize(data)
                context.contentResolver.openOutputStream(destinationUri)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                } ?: return@withContext BackupResult.Failure("Cannot open output stream.")

                // Update last backup timestamp
                settingsPreferences.setLastBackupTimestamp(System.currentTimeMillis())

                BackupResult.Success(
                    filePath = destinationUri.toString(),
                    subjectsCount = data.subjects.size,
                    schedulesCount = data.schedules.size,
                    attendanceCount = data.attendanceRecords.size
                )
            } catch (e: Exception) {
                BackupResult.Failure(
                    backupManager.classifyError(e).message ?: "Export failed."
                )
            }
        }

    // -------------------------------------------------------------------------
    // Import
    // -------------------------------------------------------------------------

    override suspend fun importBackup(sourceUri: Uri, options: RestoreOptions): RestoreResult =
        withContext(Dispatchers.IO) {
            when (val parsed = readAndParse(sourceUri)) {
                is BackupParseResult.Rejected -> RestoreResult.Failure(parsed.error.message ?: "Invalid backup.")
                is BackupParseResult.Parsed -> applyRestore(parsed.data, options)
            }
        }

    override suspend fun validateBackup(sourceUri: Uri): ValidationResult =
        withContext(Dispatchers.IO) {
            when (val parsed = readAndParse(sourceUri)) {
                is BackupParseResult.Rejected ->
                    ValidationResult.Invalid(listOf(parsed.error.message ?: "Invalid backup."))
                is BackupParseResult.Parsed -> ValidationResult.Valid(parsed.data)
            }
        }

    override suspend fun previewBackup(sourceUri: Uri): Result<BackupData> =
        withContext(Dispatchers.IO) {
            when (val parsed = readAndParse(sourceUri)) {
                is BackupParseResult.Rejected -> Result.failure(parsed.error)
                is BackupParseResult.Parsed -> Result.success(parsed.data)
            }
        }

    // -------------------------------------------------------------------------
    // Snapshot
    // -------------------------------------------------------------------------

    override suspend fun getBackupData(): BackupData = withContext(Dispatchers.IO) {
        val subjects = subjectDao.getSubjects().map { e ->
            BackupSubjectDto(e.id, e.name, e.facultyName, e.color,
                e.requiredAttendancePercentage, e.personalAttendanceGoal, e.createdAt, e.updatedAt)
        }
        val schedules = scheduleDao.getAllSchedules().map { e ->
            BackupScheduleDto(e.id, e.subjectId, e.dayOfWeek.name, e.startTime.toString(),
                e.endTime.toString(), e.room, e.teacherOverride, e.versionId, e.createdAt, e.updatedAt)
        }
        val semesters = semesterDao.getVersions().map { e ->
            BackupSemesterDto(e.id, e.name, e.isActive, e.createdAt)
        }
        val attendance = attendanceDao.getAllAttendance().map { e ->
            BackupAttendanceDto(e.id, e.subjectId, e.scheduleId, e.date.toString(),
                e.status.name, e.remarks, e.createdAt, e.updatedAt)
        }
        val theme = settingsPreferences.themeFlow.first()
        val notif = settingsPreferences.notificationsFlow.first()
        val morning = settingsPreferences.morningReminderFlow.first()
        val attendRem = settingsPreferences.attendanceReminderFlow.first()
        val missed = settingsPreferences.missedReminderFlow.first()
        val settings = BackupSettingsDto(theme, notif, morning, attendRem, missed)

        BackupData(
            metadata = BackupMetadata(),
            subjects = subjects,
            schedules = schedules,
            semesterVersions = semesters,
            attendanceRecords = attendance,
            settings = settings
        )
    }

    // -------------------------------------------------------------------------
    // Metadata
    // -------------------------------------------------------------------------

    override suspend fun getLastBackupTimestamp(): Long =
        settingsPreferences.lastBackupFlow.first()

    // -------------------------------------------------------------------------
    // Apply Restore
    // -------------------------------------------------------------------------

    override suspend fun applyRestore(data: BackupData, options: RestoreOptions): RestoreResult =
        withContext(Dispatchers.IO) {
            try {
                var subjectsRestored = 0
                var schedulesRestored = 0
                var attendanceRestored = 0
                var semestersRestored = 0

                if (options.restoreSubjects) {
                    data.subjects.forEach { dto ->
                        subjectDao.upsertSubject(
                            SubjectEntity(dto.id, dto.name, dto.facultyName, dto.color,
                                dto.requiredAttendancePercentage, dto.personalAttendanceGoal,
                                dto.createdAt, dto.updatedAt)
                        )
                        subjectsRestored++
                    }
                }

                if (options.restoreSemesterVersions) {
                    data.semesterVersions.forEach { dto ->
                        semesterDao.upsertVersion(
                            SemesterVersionEntity(dto.id, dto.name, dto.isActive, dto.createdAt)
                        )
                        semestersRestored++
                    }
                }

                if (options.restoreSchedules) {
                    data.schedules.forEach { dto ->
                        val day = runCatching { WeekDay.valueOf(dto.dayOfWeek) }.getOrNull()
                            ?: return@forEach
                        val start = runCatching { LocalTime.parse(dto.startTime) }.getOrNull()
                            ?: return@forEach
                        val end = runCatching { LocalTime.parse(dto.endTime) }.getOrNull()
                            ?: return@forEach
                        scheduleDao.upsertSchedule(
                            ScheduleEntity(dto.id, dto.subjectId, day, start, end,
                                dto.room, dto.teacherOverride, dto.versionId, dto.createdAt, dto.updatedAt)
                        )
                        schedulesRestored++
                    }
                }

                if (options.restoreAttendance) {
                    data.attendanceRecords.forEach { dto ->
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

                if (options.restoreSettings) {
                    settingsPreferences.setThemeMode(data.settings.themeMode)
                    settingsPreferences.setNotificationsEnabled(data.settings.notificationsEnabled)
                    settingsPreferences.setMorningReminderEnabled(data.settings.morningReminderEnabled)
                    settingsPreferences.setAttendanceReminderEnabled(data.settings.attendanceReminderEnabled)
                    settingsPreferences.setMissedReminderEnabled(data.settings.missedReminderEnabled)
                }

                RestoreResult.Success(subjectsRestored, schedulesRestored, attendanceRestored, semestersRestored)
            } catch (e: Exception) {
                RestoreResult.Failure(e.message ?: "Restore failed.")
            }
        }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Reads a JSON file from [uri] and parses/validates it via [BackupManager]. */
    private fun readAndParse(uri: Uri): BackupParseResult {
        val jsonResult = readJsonFromUri(uri)
        if (jsonResult.isFailure) {
            return BackupParseResult.Rejected(
                backupManager.classifyError(jsonResult.exceptionOrNull())
            )
        }
        return backupManager.parseBackup(jsonResult.getOrThrow())
    }

    private fun readJsonFromUri(uri: Uri): Result<String> {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            }
            if (content.isNullOrBlank()) {
                Result.failure(Exception("Backup file is empty or unreadable."))
            } else {
                Result.success(content)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
