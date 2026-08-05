package com.attendance.tracker.data.integrity

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
import com.attendance.tracker.domain.model.IntegrityCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class FakeSubjectDao : SubjectDao {
    var list: List<SubjectEntity> = emptyList()
    override fun observeSubjects(): Flow<List<SubjectEntity>> = flowOf(list)
    override suspend fun getSubjects(): List<SubjectEntity> = list
    override suspend fun getSubject(id: Long): SubjectEntity? = list.find { it.id == id }
    override suspend fun searchSubjects(query: String): List<SubjectEntity> = list.filter { it.name.contains(query) }
    override suspend fun countSubjects(): Int = list.size
    override suspend fun insertSubject(subject: SubjectEntity): Long = 0L
    override suspend fun updateSubject(subject: SubjectEntity): Int = 0
    override suspend fun deleteSubject(subject: SubjectEntity): Int = 0
    override suspend fun deleteAllSubjects(): Int = list.size.also { list = emptyList() }
    override suspend fun upsertSubject(subject: SubjectEntity): Long = 0L
}

class FakeScheduleDao : ScheduleDao {
    var list: List<ScheduleEntity> = emptyList()
    override suspend fun insertSchedule(schedule: ScheduleEntity): Long = 0L
    override suspend fun updateSchedule(schedule: ScheduleEntity): Int = 0
    override suspend fun deleteSchedule(schedule: ScheduleEntity): Int = 0
    override suspend fun getSchedule(id: Long): ScheduleEntity? = list.find { it.id == id }
    override fun observeSchedule(id: Long): Flow<ScheduleEntity?> = flowOf(list.find { it.id == id })
    override fun observeSchedulesForVersion(versionId: Long): Flow<List<ScheduleEntity>> = flowOf(list.filter { it.versionId == versionId })
    override suspend fun getSchedulesForVersion(versionId: Long): List<ScheduleEntity> = list.filter { it.versionId == versionId }
    override fun observeTodaySchedules(versionId: Long, day: WeekDay): Flow<List<ScheduleEntity>> = flowOf(emptyList())
    override suspend fun getSchedulesBySubject(subjectId: Long): List<ScheduleEntity> = list.filter { it.subjectId == subjectId }
    override suspend fun getSchedulesByDay(versionId: Long, day: WeekDay): List<ScheduleEntity> = emptyList()
    override suspend fun searchSchedules(versionId: Long, query: String): List<ScheduleEntity> = emptyList()
    override suspend fun checkConflicts(versionId: Long, day: WeekDay, start: LocalTime, end: LocalTime): List<ScheduleEntity> = emptyList()
    override suspend fun deleteAllSchedules(): Int = list.size.also { list = emptyList() }
    override suspend fun getAllSchedules(): List<ScheduleEntity> = list
    override suspend fun upsertSchedule(schedule: ScheduleEntity): Long = 0L
}

class FakeSemesterDao : SemesterDao {
    var list: List<SemesterVersionEntity> = emptyList()
    override suspend fun insertVersion(version: SemesterVersionEntity): Long = 0L
    override suspend fun updateVersion(version: SemesterVersionEntity): Int = 0
    override suspend fun deleteVersion(version: SemesterVersionEntity): Int = 0
    override suspend fun getVersion(id: Long): SemesterVersionEntity? = list.find { it.id == id }
    override suspend fun getVersions(): List<SemesterVersionEntity> = list
    override fun observeVersions(): Flow<List<SemesterVersionEntity>> = flowOf(list)
    override fun observeActiveVersion(): Flow<SemesterVersionEntity?> = flowOf(list.find { it.isActive })
    override suspend fun getActiveVersion(): SemesterVersionEntity? = list.find { it.isActive }
    override suspend fun deactivateAllVersions() {}
    override suspend fun activateVersion(versionId: Long) {}
    override suspend fun upsertVersion(version: SemesterVersionEntity): Long = 0L
    override suspend fun deleteAllVersions(): Int = list.size.also { list = emptyList() }
}

class FakeAttendanceDao : AttendanceDao {
    var list: List<AttendanceEntity> = emptyList()
    override suspend fun insert(attendance: AttendanceEntity): Long = 0L
    override suspend fun update(attendance: AttendanceEntity): Int = 0
    override suspend fun delete(attendance: AttendanceEntity): Int = 0
    override suspend fun getAttendanceById(id: Long): AttendanceEntity? = list.find { it.id == id }
    override fun observeAttendanceById(id: Long): Flow<AttendanceEntity?> = flowOf(list.find { it.id == id })
    override fun getAttendanceHistory(): Flow<List<AttendanceEntity>> = flowOf(list)
    override fun getAttendanceBySubject(subjectId: Long): Flow<List<AttendanceEntity>> = flowOf(list.filter { it.subjectId == subjectId })
    override fun getAttendanceByDate(date: LocalDate): Flow<List<AttendanceEntity>> = flowOf(list.filter { it.date == date })
    override suspend fun getAttendanceByDateSync(date: LocalDate): List<AttendanceEntity> = list.filter { it.date == date }
    override suspend fun getAttendanceBySubjectSync(subjectId: Long): List<AttendanceEntity> = list.filter { it.subjectId == subjectId }
    override fun observeTodayAttendance(date: LocalDate): Flow<List<AttendanceEntity>> = flowOf(emptyList())
    override fun searchAttendance(query: String): Flow<List<AttendanceEntity>> = flowOf(emptyList())
    override suspend fun checkDuplicateAttendance(subjectId: Long, scheduleId: Long, date: LocalDate): Boolean = false
    override fun countPresent(): Flow<Int> = flowOf(list.count { it.status == AttendanceStatus.PRESENT })
    override fun countAbsent(): Flow<Int> = flowOf(list.count { it.status == AttendanceStatus.ABSENT })
    override fun countCancelled(): Flow<Int> = flowOf(list.count { it.status == AttendanceStatus.CANCELLED })
    override suspend fun deleteAllAttendance(): Int = list.size.also { list = emptyList() }
    override suspend fun getAllAttendance(): List<AttendanceEntity> = list
    override suspend fun upsertAttendance(attendance: AttendanceEntity): Long = 0L
}

/**
 * Tests for [DataIntegrityVerifier] using in-memory DAO fakes.
 */
class DataIntegrityVerifierTest {

    private lateinit var subjectDao: FakeSubjectDao
    private lateinit var scheduleDao: FakeScheduleDao
    private lateinit var semesterDao: FakeSemesterDao
    private lateinit var attendanceDao: FakeAttendanceDao
    private lateinit var verifier: DataIntegrityVerifier

    @Before
    fun setUp() {
        subjectDao = FakeSubjectDao()
        scheduleDao = FakeScheduleDao()
        semesterDao = FakeSemesterDao()
        attendanceDao = FakeAttendanceDao()
        
        val testDispatcherProvider = object : com.attendance.tracker.core.common.DispatcherProvider {
            override val main = kotlinx.coroutines.test.UnconfinedTestDispatcher()
            override val io = kotlinx.coroutines.test.UnconfinedTestDispatcher()
            override val default = kotlinx.coroutines.test.UnconfinedTestDispatcher()
        }
        verifier = DataIntegrityVerifier(subjectDao, scheduleDao, semesterDao, attendanceDao, testDispatcherProvider)
    }

    @Test
    fun testEmptyDatabase_isHealthy() = runTest {
        val report = verifier.verify()
        assertTrue(report.isHealthy)
    }

    @Test
    fun testOrphanSchedule_flagged() = runTest {
        scheduleDao.list = listOf(
            ScheduleEntity(id = 1L, subjectId = 99L, dayOfWeek = WeekDay.Monday,
                startTime = LocalTime.NOON, endTime = LocalTime.NOON.plusHours(1), versionId = 5L)
        )
        val report = verifier.verify()
        assertFalse(report.isHealthy)
        assertTrue(report.issues.any { it.category == IntegrityCategory.MISSING_SUBJECTS })
        assertTrue(report.issues.any { it.affectedCount == 1 })
    }

    @Test
    fun testScheduleMissingSemesterVersion_flagged() = runTest {
        subjectDao.list = listOf(SubjectEntity(id = 1L, name = "Math"))
        scheduleDao.list = listOf(
            ScheduleEntity(id = 1L, subjectId = 1L, dayOfWeek = WeekDay.Monday,
                startTime = LocalTime.NOON, endTime = LocalTime.NOON.plusHours(1), versionId = 404L)
        )
        val report = verifier.verify()
        assertTrue(report.issues.any { it.category == IntegrityCategory.MISSING_SEMESTER_VERSIONS })
    }

    @Test
    fun testOrphanAttendanceBySubject_flagged() = runTest {
        scheduleDao.list = listOf(
            ScheduleEntity(id = 1L, subjectId = 1L, dayOfWeek = WeekDay.Monday,
                startTime = LocalTime.NOON, endTime = LocalTime.NOON.plusHours(1), versionId = 1L)
        )
        semesterDao.list = listOf(SemesterVersionEntity(id = 1L, name = "Fall 2026"))
        attendanceDao.list = listOf(
            AttendanceEntity(id = 1L, subjectId = 404L, scheduleId = 1L, date = LocalDate.now(), status = AttendanceStatus.PRESENT)
        )
        val report = verifier.verify()
        assertTrue(report.issues.any { it.category == IntegrityCategory.ORPHAN_ATTENDANCE })
    }

    @Test
    fun testOrphanAttendanceBySchedule_flagged() = runTest {
        subjectDao.list = listOf(SubjectEntity(id = 1L, name = "Math"))
        attendanceDao.list = listOf(
            AttendanceEntity(id = 1L, subjectId = 1L, scheduleId = 404L, date = LocalDate.now(), status = AttendanceStatus.PRESENT)
        )
        val report = verifier.verify()
        assertTrue(report.issues.any { it.category == IntegrityCategory.ORPHAN_ATTENDANCE })
    }

    @Test
    fun testDuplicateSubjectNames_flagged() = runTest {
        subjectDao.list = listOf(
            SubjectEntity(id = 1L, name = "Physics"),
            SubjectEntity(id = 2L, name = "physics")
        )
        val report = verifier.verify()
        assertTrue(report.issues.any { it.category == IntegrityCategory.DUPLICATE_IDS })
    }

    @Test
    fun testDuplicateAttendanceKeys_flagged() = runTest {
        subjectDao.list = listOf(SubjectEntity(id = 1L, name = "Math"))
        scheduleDao.list = listOf(
            ScheduleEntity(id = 1L, subjectId = 1L, dayOfWeek = WeekDay.Monday,
                startTime = LocalTime.NOON, endTime = LocalTime.NOON.plusHours(1), versionId = 1L)
        )
        semesterDao.list = listOf(SemesterVersionEntity(id = 1L, name = "Fall 2026"))
        val date = LocalDate.now()
        attendanceDao.list = listOf(
            AttendanceEntity(id = 1L, subjectId = 1L, scheduleId = 1L, date = date, status = AttendanceStatus.PRESENT),
            AttendanceEntity(id = 2L, subjectId = 1L, scheduleId = 1L, date = date, status = AttendanceStatus.ABSENT)
        )
        val report = verifier.verify()
        assertTrue(report.issues.any { it.category == IntegrityCategory.DUPLICATE_IDS })
    }

    @Test
    fun testConsistentDatabase_isHealthy() = runTest {
        subjectDao.list = listOf(SubjectEntity(id = 1L, name = "Math"))
        semesterDao.list = listOf(SemesterVersionEntity(id = 1L, name = "Fall 2026"))
        scheduleDao.list = listOf(
            ScheduleEntity(id = 1L, subjectId = 1L, dayOfWeek = WeekDay.Monday,
                startTime = LocalTime.NOON, endTime = LocalTime.NOON.plusHours(1), versionId = 1L)
        )
        attendanceDao.list = listOf(
            AttendanceEntity(id = 1L, subjectId = 1L, scheduleId = 1L, date = LocalDate.now(), status = AttendanceStatus.PRESENT)
        )
        val report = verifier.verify()
        assertTrue(report.isHealthy)
    }
}
