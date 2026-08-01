package com.attendance.tracker.domain.usecase.planner

import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.model.Subject
import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.domain.model.SemesterVersion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class LocalFakeAttendanceRepository : com.attendance.tracker.domain.repository.AttendanceRepository {
    private val _list = MutableStateFlow<List<Attendance>>(emptyList())

    override fun observeAttendanceForSubject(subjectId: Long): Flow<List<Attendance>> {
        return _list.map { it.filter { s -> s.subjectId == subjectId } }
    }

    override fun observeAttendanceForDate(date: LocalDate): Flow<List<Attendance>> {
        return _list.map { it.filter { s -> s.date == date } }
    }

    override fun observeTodayAttendance(date: LocalDate): Flow<List<Attendance>> {
        return _list.map { it.filter { s -> s.date == date } }
    }

    override fun observeAttendanceHistory(): Flow<List<Attendance>> {
        return _list.map { it.sortedByDescending { s -> s.date } }
    }

    override fun observeAttendanceById(id: Long): Flow<Attendance?> {
        return _list.map { it.find { s -> s.id == id } }
    }

    override suspend fun getAttendanceById(id: Long): Attendance? = _list.value.find { it.id == id }

    override suspend fun getAttendanceForDateSync(date: LocalDate): List<Attendance> = _list.value.filter { it.date == date }

    override suspend fun insertAttendance(attendance: Attendance): Long {
        val current = _list.value.toMutableList()
        val id = if (attendance.id == 0L) (current.size + 1).toLong() else attendance.id
        current.removeAll { it.id == id }
        current.add(attendance.copy(id = id))
        _list.value = current
        return id
    }

    override suspend fun updateAttendance(attendance: Attendance): Int {
        val current = _list.value.toMutableList()
        val idx = current.indexOfFirst { it.id == attendance.id }
        return if (idx != -1) {
            current[idx] = attendance
            _list.value = current
            1
        } else {
            0
        }
    }

    override suspend fun deleteAttendance(attendance: Attendance): Int {
        val current = _list.value.toMutableList()
        return if (current.removeIf { it.id == attendance.id }) {
            _list.value = current
            1
        } else {
            0
        }
    }

    override suspend fun checkDuplicateAttendance(subjectId: Long, scheduleId: Long, date: LocalDate): Boolean {
        return _list.value.any { it.subjectId == subjectId && it.scheduleId == scheduleId && it.date == date }
    }

    override fun countPresent(): Flow<Int> = _list.map { it.count { it.status == AttendanceStatus.PRESENT } }
    override fun countAbsent(): Flow<Int> = _list.map { it.count { it.status == AttendanceStatus.ABSENT } }
    override fun countCancelled(): Flow<Int> = _list.map { it.count { it.status == AttendanceStatus.CANCELLED } }
    override fun searchAttendance(query: String): Flow<List<Attendance>> = _list.map {
        it.filter { s -> s.remarks?.contains(query, ignoreCase = true) == true }
    }
}

class LocalFakeSubjectRepository : com.attendance.tracker.domain.repository.SubjectRepository {
    private val _list = MutableStateFlow<List<Subject>>(emptyList())
    override fun observeSubjects(): Flow<List<Subject>> = _list
    override suspend fun getSubjects(): List<Subject> = _list.value
    override suspend fun getSubjectById(id: Long): Subject? = _list.value.find { it.id == id }
    override suspend fun insertSubject(subject: Subject): Long {
        val current = _list.value.toMutableList()
        val id = if (subject.id == 0L) (current.size + 1).toLong() else subject.id
        current.add(subject.copy(id = id))
        _list.value = current
        return id
    }
    override suspend fun updateSubject(subject: Subject): Int = 0
    override suspend fun deleteSubject(subject: Subject): Int = 0
    override suspend fun searchSubjects(query: String): List<Subject> = _list.value.filter { it.name.contains(query, ignoreCase = true) }
    override suspend fun countSubjects(): Int = _list.value.size
}

class LocalFakeScheduleRepository : com.attendance.tracker.domain.repository.ScheduleRepository {
    private val _list = MutableStateFlow<List<Schedule>>(emptyList())
    override fun observeSchedulesForVersion(versionId: Long): Flow<List<Schedule>> = _list.map { it.filter { s -> s.versionId == versionId } }
    override fun observeSchedule(id: Long): Flow<Schedule?> = _list.map { it.find { s -> s.id == id } }
    override suspend fun getSchedule(id: Long): Schedule? = _list.value.find { it.id == id }
    override suspend fun getSchedulesForVersion(versionId: Long): List<Schedule> = _list.value.filter { it.versionId == versionId }
    override fun observeTodaySchedules(versionId: Long, day: WeekDay): Flow<List<Schedule>> = _list.map { it.filter { s -> s.versionId == versionId && s.dayOfWeek == day } }
    override suspend fun getSchedulesBySubject(subjectId: Long): List<Schedule> = _list.value.filter { it.subjectId == subjectId }
    override suspend fun getSchedulesByDay(versionId: Long, day: WeekDay): List<Schedule> = _list.value.filter { it.versionId == versionId && it.dayOfWeek == day }
    override suspend fun searchSchedules(versionId: Long, query: String): List<Schedule> = emptyList()
    override suspend fun checkConflicts(versionId: Long, day: WeekDay, start: LocalTime, end: LocalTime): List<Schedule> = emptyList()
    override suspend fun insertSchedule(schedule: Schedule): Long {
        val current = _list.value.toMutableList()
        val id = if (schedule.id == 0L) (current.size + 1).toLong() else schedule.id
        current.add(schedule.copy(id = id))
        _list.value = current
        return id
    }
    override suspend fun updateSchedule(schedule: Schedule): Int = 0
    override suspend fun deleteSchedule(schedule: Schedule): Int = 0
}

class LeavePlannerUseCaseTest {

    private lateinit var subjectRepo: LocalFakeSubjectRepository
    private lateinit var scheduleRepo: LocalFakeScheduleRepository
    private lateinit var attendanceRepo: LocalFakeAttendanceRepository
    private lateinit var leavePlanner: LeavePlannerUseCase

    @Before
    fun setUp() {
        subjectRepo = LocalFakeSubjectRepository()
        scheduleRepo = LocalFakeScheduleRepository()
        attendanceRepo = LocalFakeAttendanceRepository()
        leavePlanner = LeavePlannerUseCase(subjectRepo, scheduleRepo, attendanceRepo)
    }

    @Test
    fun testLeavePlanner_calculatesProjectedDrops() = runTest {
        val subjId = subjectRepo.insertSubject(
            Subject(id = 1L, name = "Software Eng", requiredAttendancePercentage = 75, personalAttendanceGoal = 85)
        )

        for (i in 1..4) {
            attendanceRepo.insertAttendance(
                Attendance(id = i.toLong(), subjectId = subjId, scheduleId = 10L, date = LocalDate.now().minusDays(i.toLong()), status = AttendanceStatus.PRESENT)
            )
        }

        scheduleRepo.insertSchedule(
            Schedule(id = 10L, subjectId = subjId, dayOfWeek = WeekDay.Monday, startTime = LocalTime.NOON, endTime = LocalTime.NOON.plusHours(1), versionId = 1L)
        )

        val nextMonday = LocalDate.now().with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY))

        val result = leavePlanner(
            selectedDates = listOf(nextMonday),
            activeVersionId = 1L
        )

        assertEquals(1, result.affectedSubjects.size)
        val affected = result.affectedSubjects.first()
        assertEquals(subjId, affected.subjectId)
        assertEquals(1, affected.missedClassesCount)
        assertEquals(100.0, affected.currentPercentage, 0.1)
        assertEquals(80.0, affected.projectedPercentage, 0.1)
        assertTrue(affected.isProjectedSafe)
        assertTrue(result.isOverallSafe)
    }
}
