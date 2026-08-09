package com.attendance.tracker.data.repository

import com.attendance.tracker.core.backup.BackupDeserializer
import com.attendance.tracker.core.backup.BackupSerializer
import com.attendance.tracker.core.common.DefaultDispatcherProvider
import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.data.local.database.dao.ArchiveDao
import com.attendance.tracker.data.local.database.dao.AttendanceDao
import com.attendance.tracker.data.local.database.dao.ScheduleDao
import com.attendance.tracker.data.local.database.dao.SemesterDao
import com.attendance.tracker.data.local.database.dao.SubjectDao
import com.attendance.tracker.data.local.database.entity.ArchiveEntity
import com.attendance.tracker.data.local.database.entity.AttendanceEntity
import com.attendance.tracker.data.local.database.entity.SubjectEntity
import com.attendance.tracker.domain.model.RestoreOptions
import com.attendance.tracker.domain.usecase.attendance.CalculateAttendanceStatisticsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.time.LocalDate

class ArchiveRepositoryImplTest {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Test
    fun createAndReadArchive_usesMl1StatisticsAndPreservesMedicalLeave() = runBlocking {
        val attendanceDao = RecordingAttendanceDao(createAttendanceRecords())
        val archiveDao = RecordingArchiveDao()
        val subjectDao = mock(SubjectDao::class.java)
        val scheduleDao = mock(ScheduleDao::class.java)
        val semesterDao = mock(SemesterDao::class.java)
        `when`(subjectDao.getSubjects()).thenReturn(
            listOf(SubjectEntity(id = 1L, name = "Mathematics"))
        )
        `when`(scheduleDao.getAllSchedules()).thenReturn(emptyList())

        val repository = repository(archiveDao, attendanceDao, subjectDao, scheduleDao, semesterDao)
        repository.createArchive("Fall 2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 1))

        val saved = archiveDao.saved
        assertNotNull(saved)
        assertEquals(100, saved?.totalClasses)
        assertEquals(70, saved?.presentCount)
        assertEquals(25, saved?.absentCount)
        assertEquals(3, saved?.cancelledCount)
        assertEquals(70.0, saved?.overallPercentage ?: 0.0, 0.01)
        assertTrue(saved?.attendanceJson?.contains("MEDICAL_LEAVE") == true)

        val archive = repository.getArchive(1L)
        assertNotNull(archive)
        assertEquals(5, archive?.medicalLeaveCount)
        assertEquals(70.0, archive?.overallPercentage ?: 0.0, 0.01)
        assertEquals(75.0, archive?.withMedicalPercentage ?: 0.0, 0.01)
        assertEquals(5, archive?.subjectStats?.single()?.medicalLeaveCount)
        assertEquals(75.0, archive?.subjectStats?.single()?.withMedicalPercentage ?: 0.0, 0.01)

        attendanceDao.records.clear()
        val restoreResult = repository.restoreArchive(
            1L,
            RestoreOptions(
                restoreSubjects = false,
                restoreSchedules = false,
                restoreSemesterVersions = false,
                restoreAttendance = true,
                restoreSettings = false
            )
        )
        assertTrue(restoreResult is com.attendance.tracker.domain.model.RestoreResult.Success)
        assertEquals(103, attendanceDao.records.size)
        assertEquals(
            5,
            attendanceDao.records.count { it.status == AttendanceStatus.MEDICAL_LEAVE }
        )
    }

    @Test
    fun archiveWithOnlyExcludedStatuses_avoidsDivisionByZero() = runBlocking {
        val records = listOf(
            AttendanceEntity(
                id = 1L,
                subjectId = 1L,
                scheduleId = 1L,
                date = LocalDate.of(2026, 1, 1),
                status = AttendanceStatus.MEDICAL_LEAVE
            ),
            AttendanceEntity(
                id = 2L,
                subjectId = 1L,
                scheduleId = 1L,
                date = LocalDate.of(2026, 1, 2),
                status = AttendanceStatus.CANCELLED
            )
        )
        val attendanceDao = RecordingAttendanceDao(records)
        val archiveDao = RecordingArchiveDao()
        val subjectDao = mock(SubjectDao::class.java)
        val scheduleDao = mock(ScheduleDao::class.java)
        val semesterDao = mock(SemesterDao::class.java)
        `when`(subjectDao.getSubjects()).thenReturn(listOf(SubjectEntity(id = 1L, name = "Mathematics")))
        `when`(scheduleDao.getAllSchedules()).thenReturn(emptyList())

        val repository = repository(archiveDao, attendanceDao, subjectDao, scheduleDao, semesterDao)
        repository.createArchive("Empty Attendance", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2))
        val archive = repository.getArchive(1L)

        assertNotNull(archive)
        assertEquals(1, archive?.totalClasses)
        assertEquals(0.0, archive?.overallPercentage ?: -1.0, 0.0)
        assertEquals(100.0, archive?.withMedicalPercentage ?: -1.0, 0.0)
        assertEquals(100.0, archive?.subjectStats?.single()?.withMedicalPercentage ?: -1.0, 0.0)
    }

    private fun repository(
        archiveDao: ArchiveDao,
        attendanceDao: AttendanceDao,
        subjectDao: SubjectDao,
        scheduleDao: ScheduleDao,
        semesterDao: SemesterDao
    ): ArchiveRepositoryImpl = ArchiveRepositoryImpl(
        archiveDao = archiveDao,
        subjectDao = subjectDao,
        scheduleDao = scheduleDao,
        semesterDao = semesterDao,
        attendanceDao = attendanceDao,
        serializer = BackupSerializer(json),
        deserializer = BackupDeserializer(json),
        calculateStatistics = CalculateAttendanceStatisticsUseCase(),
        dispatcherProvider = DefaultDispatcherProvider()
    )

    private fun createAttendanceRecords(): MutableList<AttendanceEntity> {
        val records = mutableListOf<AttendanceEntity>()
        repeat(70) { index -> records += attendance(index + 1, AttendanceStatus.PRESENT) }
        repeat(25) { index -> records += attendance(index + 71, AttendanceStatus.ABSENT) }
        repeat(5) { index -> records += attendance(index + 96, AttendanceStatus.MEDICAL_LEAVE) }
        repeat(3) { index -> records += attendance(index + 101, AttendanceStatus.CANCELLED) }
        return records
    }

    private fun attendance(id: Int, status: AttendanceStatus) = AttendanceEntity(
        id = id.toLong(),
        subjectId = 1L,
        scheduleId = 1L,
        date = LocalDate.of(2026, 1, 1).plusDays(id.toLong()),
        status = status
    )

    private class RecordingArchiveDao : ArchiveDao {
        var saved: ArchiveEntity? = null

        override suspend fun insertArchive(archive: ArchiveEntity): Long {
            saved = archive.copy(id = 1L)
            return 1L
        }

        override fun observeArchives(): Flow<List<ArchiveEntity>> = flowOf(saved?.let { listOf(it) } ?: emptyList())

        override suspend fun getAllArchives(): List<ArchiveEntity> = saved?.let { listOf(it) } ?: emptyList()

        override suspend fun getArchiveById(id: Long): ArchiveEntity? = saved?.takeIf { it.id == id }

        override suspend fun deleteArchiveById(id: Long): Int {
            val existed = saved?.id == id
            if (existed) saved = null
            return if (existed) 1 else 0
        }

        override suspend fun deleteAllArchives() {
            saved = null
        }
    }

    private class RecordingAttendanceDao(initial: List<AttendanceEntity>) : AttendanceDao {
        val records = initial.toMutableList()

        override suspend fun insert(attendance: AttendanceEntity): Long {
            records += attendance
            return attendance.id
        }

        override suspend fun update(attendance: AttendanceEntity): Int {
            val index = records.indexOfFirst { it.id == attendance.id }
            if (index < 0) return 0
            records[index] = attendance
            return 1
        }

        override suspend fun delete(attendance: AttendanceEntity): Int = if (records.removeIf { it.id == attendance.id }) 1 else 0
        override suspend fun getAttendanceById(id: Long): AttendanceEntity? = records.find { it.id == id }
        override fun observeAttendanceById(id: Long): Flow<AttendanceEntity?> = flowOf(records.find { it.id == id })
        override fun getAttendanceHistory(): Flow<List<AttendanceEntity>> = flowOf(records)
        override fun getAttendanceBySubject(subjectId: Long): Flow<List<AttendanceEntity>> = flowOf(records.filter { it.subjectId == subjectId })
        override suspend fun getAttendanceBySubjectSync(subjectId: Long): List<AttendanceEntity> = records.filter { it.subjectId == subjectId }
        override fun getAttendanceByDate(date: LocalDate): Flow<List<AttendanceEntity>> = flowOf(records.filter { it.date == date })
        override suspend fun getAttendanceByDateSync(date: LocalDate): List<AttendanceEntity> = records.filter { it.date == date }
        override fun observeTodayAttendance(date: LocalDate): Flow<List<AttendanceEntity>> = flowOf(records.filter { it.date == date })
        override fun searchAttendance(query: String): Flow<List<AttendanceEntity>> = flowOf(records)
        override suspend fun checkDuplicateAttendance(subjectId: Long, scheduleId: Long, date: LocalDate): Boolean = records.any { it.subjectId == subjectId && it.scheduleId == scheduleId && it.date == date }
        override fun countPresent(): Flow<Int> = flowOf(records.count { it.status == AttendanceStatus.PRESENT })
        override fun countAbsent(): Flow<Int> = flowOf(records.count { it.status == AttendanceStatus.ABSENT })
        override fun countCancelled(): Flow<Int> = flowOf(records.count { it.status == AttendanceStatus.CANCELLED })
        override suspend fun deleteAllAttendance(): Int = records.size.also { records.clear() }
        override suspend fun getAllAttendance(): List<AttendanceEntity> = records.toList()
        override suspend fun upsertAttendance(attendance: AttendanceEntity): Long {
            val index = records.indexOfFirst { it.id == attendance.id }
            if (index >= 0) records[index] = attendance else records += attendance
            return attendance.id
        }
    }
}
