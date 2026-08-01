package com.attendance.tracker.data.repository

import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.data.local.database.entity.ScheduleEntity
import com.attendance.tracker.data.local.datasource.ScheduleLocalDataSource
import com.attendance.tracker.domain.model.Schedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

class FakeScheduleLocalDataSource : ScheduleLocalDataSource {
    val list = mutableListOf<ScheduleEntity>()

    override fun observeSchedulesForVersion(versionId: Long): Flow<List<ScheduleEntity>> = flow {
        emit(list.filter { it.versionId == versionId })
    }

    override fun observeSchedule(id: Long): Flow<ScheduleEntity?> = flow {
        emit(list.find { it.id == id })
    }

    override suspend fun getSchedule(id: Long): ScheduleEntity? = list.find { it.id == id }

    override suspend fun getSchedulesForVersion(versionId: Long): List<ScheduleEntity> = list.filter { it.versionId == versionId }

    override fun observeTodaySchedules(versionId: Long, day: WeekDay): Flow<List<ScheduleEntity>> = flow {
        emit(list.filter { it.versionId == versionId && it.dayOfWeek == day })
    }

    override suspend fun getSchedulesBySubject(subjectId: Long): List<ScheduleEntity> = list.filter { it.subjectId == subjectId }

    override suspend fun getSchedulesByDay(versionId: Long, day: WeekDay): List<ScheduleEntity> = list.filter { it.versionId == versionId && it.dayOfWeek == day }

    override suspend fun searchSchedules(versionId: Long, query: String): List<ScheduleEntity> = list.filter {
        it.versionId == versionId && (it.room?.contains(query) == true || it.teacherOverride?.contains(query) == true)
    }

    override suspend fun checkConflicts(
        versionId: Long,
        day: WeekDay,
        start: LocalTime,
        end: LocalTime
    ): List<ScheduleEntity> {
        return list.filter {
            it.versionId == versionId && it.dayOfWeek == day &&
                    it.startTime.isBefore(end) && it.endTime.isAfter(start)
        }
    }

    override suspend fun insertSchedule(schedule: ScheduleEntity): Long {
        list.add(schedule)
        return schedule.id
    }

    override suspend fun updateSchedule(schedule: ScheduleEntity): Int {
        val idx = list.indexOfFirst { it.id == schedule.id }
        return if (idx != -1) {
            list[idx] = schedule
            1
        } else {
            0
        }
    }

    override suspend fun deleteSchedule(schedule: ScheduleEntity): Int {
        return if (list.removeIf { it.id == schedule.id }) 1 else 0
    }
}

class ScheduleRepositoryImplTest {

    private lateinit var dataSource: FakeScheduleLocalDataSource
    private lateinit var repository: ScheduleRepositoryImpl

    @Before
    fun setUp() {
        dataSource = FakeScheduleLocalDataSource()
        repository = ScheduleRepositoryImpl(dataSource)
    }

    @Test
    fun testInsertAndGetSchedule_mapsAndDelegates() = runTest {
        val schedule = Schedule(
            id = 22L,
            subjectId = 1L,
            dayOfWeek = WeekDay.Monday,
            startTime = LocalTime.NOON,
            endTime = LocalTime.NOON.plusHours(1),
            room = "201",
            versionId = 2L
        )

        repository.insertSchedule(schedule)
        assertEquals(1, dataSource.list.size)
        assertEquals("201", dataSource.list.first().room)

        val fetched = repository.getSchedule(22L)
        assertEquals("201", fetched?.room)
    }
}
