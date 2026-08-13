package com.attendance.tracker.data.integrity

import com.attendance.tracker.data.local.database.dao.AttendanceDao
import com.attendance.tracker.data.local.database.dao.ScheduleDao
import com.attendance.tracker.data.local.database.dao.SemesterDao
import com.attendance.tracker.data.local.database.dao.SubjectDao
import com.attendance.tracker.data.local.database.entity.AttendanceEntity
import com.attendance.tracker.data.local.database.entity.SubjectEntity
import com.attendance.tracker.domain.model.IntegrityCategory
import com.attendance.tracker.domain.model.IntegrityIssue
import com.attendance.tracker.domain.model.IntegrityReport
import com.attendance.tracker.domain.model.IntegritySeverity
import com.attendance.tracker.core.common.DispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans the local Room database for data-integrity problems.
 *
 * Checks performed:
 * 1. Foreign keys: schedules referencing missing subjects or semester versions.
 * 2. Orphan attendance records referencing missing subjects or schedules.
 * 3. Duplicate subject names and duplicate attendance keys.
 *
 * The verifier is read-only; repair is left to the user via [IntegrityIssue.repairSuggestion].
 */
@Singleton
class DataIntegrityVerifier @Inject constructor(
    private val subjectDao: SubjectDao,
    private val scheduleDao: ScheduleDao,
    private val semesterDao: SemesterDao,
    private val attendanceDao: AttendanceDao,
    private val dispatcherProvider: DispatcherProvider
) {

    /**
     * Executes a full integrity scan on the live database.
     */
    suspend fun verify(): IntegrityReport = withContext(dispatcherProvider.io) {
        val subjects = subjectDao.getSubjects()
        val schedules = scheduleDao.getAllSchedules()
        val semesters = semesterDao.getVersions()
        val attendance = attendanceDao.getAllAttendance()

        val issues = mutableListOf<IntegrityIssue>()

        val subjectIds = subjects.map { it.id }.toSet()
        val scheduleIds = schedules.map { it.id }.toSet()
        val semesterIds = semesters.map { it.id }.toSet()

        // 1. Foreign keys: schedules referencing missing subjects.
        val schedulesMissingSubject = schedules.filter { it.subjectId !in subjectIds }
        if (schedulesMissingSubject.isNotEmpty()) {
            issues += IntegrityIssue(
                category = IntegrityCategory.MISSING_SUBJECTS,
                severity = IntegritySeverity.CRITICAL,
                message = "${schedulesMissingSubject.size} schedule(s) reference subjects that no longer exist.",
                affectedCount = schedulesMissingSubject.size,
                repairSuggestion = "Restore the missing subjects from a backup/archive, or delete these schedules."
            )
        }

        // 2. Foreign keys: schedules referencing missing semester versions.
        val schedulesMissingSemester = schedules.filter { it.versionId !in semesterIds }
        if (schedulesMissingSemester.isNotEmpty()) {
            issues += IntegrityIssue(
                category = IntegrityCategory.MISSING_SEMESTER_VERSIONS,
                severity = IntegritySeverity.CRITICAL,
                message = "${schedulesMissingSemester.size} schedule(s) reference semester versions that no longer exist.",
                affectedCount = schedulesMissingSemester.size,
                repairSuggestion = "Create the missing semester version, or restore it from a backup/archive."
            )
        }

        // 3. Orphan attendance records referencing missing subjects.
        val orphanBySubject = attendance.filter { it.subjectId !in subjectIds }
        if (orphanBySubject.isNotEmpty()) {
            issues += IntegrityIssue(
                category = IntegrityCategory.ORPHAN_ATTENDANCE,
                severity = IntegritySeverity.CRITICAL,
                message = "${orphanBySubject.size} attendance record(s) reference missing subjects.",
                affectedCount = orphanBySubject.size,
                repairSuggestion = "Restore the missing subjects, or delete the orphan attendance records."
            )
        }

        // 4. Orphan attendance records referencing missing schedules.
        val orphanBySchedule = attendance.filter { it.scheduleId !in scheduleIds }
        if (orphanBySchedule.isNotEmpty()) {
            issues += IntegrityIssue(
                category = IntegrityCategory.ORPHAN_ATTENDANCE,
                severity = IntegritySeverity.CRITICAL,
                message = "${orphanBySchedule.size} attendance record(s) reference missing schedules.",
                affectedCount = orphanBySchedule.size,
                repairSuggestion = "Restore the missing schedules, or delete the orphan attendance records."
            )
        }

        // 5. Missing schedules referenced by attendance (defensive; overlaps with #4).
        val attendanceWithMissingSchedule = attendance.filter { it.scheduleId !in scheduleIds }
        if (attendanceWithMissingSchedule.isNotEmpty() && orphanBySchedule.isEmpty()) {
            issues += IntegrityIssue(
                category = IntegrityCategory.MISSING_SCHEDULES,
                severity = IntegritySeverity.WARNING,
                message = "${attendanceWithMissingSchedule.size} attendance record(s) reference schedules that no longer exist.",
                affectedCount = attendanceWithMissingSchedule.size,
                repairSuggestion = "Restore the referenced schedules from a backup/archive."
            )
        }

        // 6. Duplicate subject names.
        duplicateSubjectNames(subjects)?.let { name ->
            issues += IntegrityIssue(
                category = IntegrityCategory.DUPLICATE_IDS,
                severity = IntegritySeverity.WARNING,
                message = "Duplicate subject name detected: \"$name\".",
                affectedCount = 1,
                repairSuggestion = "Rename one of the duplicate subjects."
            )
        }

        // 7. Duplicate attendance keys (subjectId + scheduleId + date).
        val keyOf: (AttendanceEntity) -> String = { "${it.subjectId}|${it.scheduleId}|${it.date}" }
        val duplicateAttendanceCount = attendance.size - attendance.distinctBy(keyOf).size
        if (duplicateAttendanceCount > 0) {
            issues += IntegrityIssue(
                category = IntegrityCategory.DUPLICATE_IDS,
                severity = IntegritySeverity.WARNING,
                message = "$duplicateAttendanceCount duplicate attendance record(s) share the same subject, schedule, and date.",
                affectedCount = duplicateAttendanceCount,
                repairSuggestion = "Review and delete the duplicate attendance records."
            )
        }

        IntegrityReport(issues = issues)
    }

    private fun duplicateSubjectNames(subjects: List<SubjectEntity>): String? {
        val names = subjects.groupBy { it.name.trim().lowercase() }
        return names.entries.firstOrNull { it.value.size > 1 }?.key
    }
}
