package com.attendance.tracker.domain.usecase.schedule

import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.domain.model.Schedule
import com.attendance.tracker.domain.model.SemesterVersion
import com.attendance.tracker.domain.repository.ScheduleRepository
import com.attendance.tracker.domain.repository.SemesterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

/**
 * A helper list that triggers a callback whenever items are added or removed,
 * ensuring flow updates are emitted reactively in tests.
 */
class ObservableList<T>(private val onUpdate: (List<T>) -> Unit) : ArrayList<T>() {
    override fun add(element: T): Boolean {
        val res = super.add(element)
        onUpdate(this.toList())
        return res
    }

    override fun addAll(elements: Collection<T>): Boolean {
        val res = super.addAll(elements)
        onUpdate(this.toList())
        return res
    }

    override fun removeIf(filter: java.util.function.Predicate<in T>): Boolean {
        val res = super.removeIf(filter)
        onUpdate(this.toList())
        return res
    }

    override fun remove(element: T): Boolean {
        val res = super.remove(element)
        onUpdate(this.toList())
        return res
    }

    override fun clear() {
        super.clear()
        onUpdate(this.toList())
    }
}

class FakeScheduleRepository : ScheduleRepository {
    private val _schedules = MutableStateFlow<List<Schedule>>(emptyList())
    val list = ObservableList<Schedule> { _schedules.value = it }

    override fun observeSchedulesForVersion(versionId: Long): Flow<List<Schedule>> {
        return _schedules.map { it.filter { s -> s.versionId == versionId } }
    }

    override fun observeSchedule(id: Long): Flow<Schedule?> {
        return _schedules.map { it.find { s -> s.id == id } }
    }

    override suspend fun getSchedule(id: Long): Schedule? = _schedules.value.find { it.id == id }

    override suspend fun getSchedulesForVersion(versionId: Long): List<Schedule> = _schedules.value.filter { it.versionId == versionId }

    override fun observeTodaySchedules(versionId: Long, day: WeekDay): Flow<List<Schedule>> {
        return _schedules.map { it.filter { it.versionId == versionId && it.dayOfWeek == day } }
    }

    override suspend fun getSchedulesBySubject(subjectId: Long): List<Schedule> = _schedules.value.filter { it.subjectId == subjectId }

    override suspend fun getSchedulesByDay(versionId: Long, day: WeekDay): List<Schedule> = _schedules.value.filter { it.versionId == versionId && it.dayOfWeek == day }

    override suspend fun searchSchedules(versionId: Long, query: String): List<Schedule> = _schedules.value.filter {
        it.versionId == versionId && (it.room?.contains(query, ignoreCase = true) == true || it.teacherOverride?.contains(query, ignoreCase = true) == true)
    }

    override suspend fun checkConflicts(
        versionId: Long,
        day: WeekDay,
        start: LocalTime,
        end: LocalTime
    ): List<Schedule> {
        return _schedules.value.filter {
            it.versionId == versionId && it.dayOfWeek == day &&
                    it.startTime.isBefore(end) && it.endTime.isAfter(start)
        }
    }

    override suspend fun insertSchedule(schedule: Schedule): Long {
        val id = if (schedule.id == 0L) (list.size + 1).toLong() else schedule.id
        list.removeIf { it.id == id }
        list.add(schedule.copy(id = id))
        return id
    }

    override suspend fun updateSchedule(schedule: Schedule): Int {
        val idx = list.indexOfFirst { it.id == schedule.id }
        return if (idx != -1) {
            list[idx] = schedule
            val copy = list.toList()
            list.clear()
            list.addAll(copy)
            1
        } else {
            0
        }
    }

    override suspend fun deleteSchedule(schedule: Schedule): Int {
        return if (list.removeIf { it.id == schedule.id }) 1 else 0
    }
}

class FakeSemesterRepository : SemesterRepository {
    private val _versions = MutableStateFlow<List<SemesterVersion>>(emptyList())
    val versions = ObservableList<SemesterVersion> { _versions.value = it }

    override fun observeVersions(): Flow<List<SemesterVersion>> = _versions

    override fun observeActiveVersion(): Flow<SemesterVersion?> = _versions.map { it.find { v -> v.isActive } }

    override suspend fun getVersions(): List<SemesterVersion> = _versions.value

    override suspend fun getVersion(id: Long): SemesterVersion? = _versions.value.find { it.id == id }

    override suspend fun getActiveVersion(): SemesterVersion? = _versions.value.find { it.isActive }

    override suspend fun insertVersion(version: SemesterVersion): Long {
        val id = if (version.id == 0L) (versions.size + 1).toLong() else version.id
        versions.add(version.copy(id = id))
        return id
    }

    override suspend fun updateVersion(version: SemesterVersion): Int {
        val idx = versions.indexOfFirst { it.id == version.id }
        return if (idx != -1) {
            versions[idx] = version
            val copy = versions.toList()
            versions.clear()
            versions.addAll(copy)
            1
        } else {
            0
        }
    }

    override suspend fun deleteVersion(version: SemesterVersion): Int {
        return if (versions.removeIf { it.id == version.id }) 1 else 0
    }

    override suspend fun switchActiveVersion(versionId: Long) {
        val updated = _versions.value.map { ver ->
            ver.copy(isActive = ver.id == versionId)
        }
        versions.clear()
        versions.addAll(updated)
    }
}

class ScheduleUseCaseTest {

    private lateinit var repository: FakeScheduleRepository
    private lateinit var semesterRepo: FakeSemesterRepository

    private lateinit var addUseCase: AddScheduleUseCase
    private lateinit var deleteUseCase: DeleteScheduleUseCase
    private lateinit var detectConflictUseCase: DetectConflictUseCase
    private lateinit var switchVersionUseCase: SwitchTimetableVersionUseCase
    private lateinit var observeUseCase: ObserveScheduleUseCase

    @Before
    fun setUp() {
        repository = FakeScheduleRepository()
        semesterRepo = FakeSemesterRepository()

        addUseCase = AddScheduleUseCase(repository)
        deleteUseCase = DeleteScheduleUseCase(repository)
        detectConflictUseCase = DetectConflictUseCase(repository)
        switchVersionUseCase = SwitchTimetableVersionUseCase(semesterRepo)
        observeUseCase = ObserveScheduleUseCase(repository)
    }

    @Test
    fun testAddSchedule_savesSchedule() = runTest {
        val schedule = Schedule(subjectId = 1L, dayOfWeek = WeekDay.Monday, startTime = LocalTime.NOON, endTime = LocalTime.NOON.plusHours(1), versionId = 3L)
        val id = addUseCase(schedule)
        assertEquals(1L, id)
        assertEquals(1, repository.list.size)
    }

    @Test
    fun testDetectConflict_identifiesOverlaps() = runTest {
        val conflict = Schedule(id = 1L, subjectId = 2L, dayOfWeek = WeekDay.Monday, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 0), versionId = 2L)
        repository.list.add(conflict)

        val overlaps = detectConflictUseCase(
            versionId = 2L,
            day = WeekDay.Monday,
            start = LocalTime.of(9, 30),
            end = LocalTime.of(10, 30)
        )
        assertEquals(1, overlaps.size)
        assertEquals(1L, overlaps.first().id)
    }

    @Test
    fun testSwitchVersion_activatesTarget() = runTest {
        val v1 = SemesterVersion(id = 1L, name = "V1", isActive = true)
        val v2 = SemesterVersion(id = 2L, name = "V2", isActive = false)
        semesterRepo.versions.addAll(listOf(v1, v2))

        switchVersionUseCase(2L)
        assertTrue(semesterRepo.versions.find { it.id == 2L }?.isActive == true)
        assertTrue(semesterRepo.versions.find { it.id == 1L }?.isActive == false)
    }
}
