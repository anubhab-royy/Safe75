package com.attendance.tracker.domain.usecase.schedule

import com.attendance.tracker.core.model.WeekDay
import com.attendance.tracker.domain.model.Schedule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalTime

class SaveMultiDayScheduleUseCaseTest {

    private lateinit var repository: FakeScheduleRepository
    private lateinit var useCase: SaveMultiDayScheduleUseCase

    @Before
    fun setUp() {
        repository = FakeScheduleRepository()
        useCase = SaveMultiDayScheduleUseCase(repository)
    }

    private val versionId = 1L
    private val start = LocalTime.of(9, 0)
    private val end = LocalTime.of(10, 30)

    @Test
    fun `create inserts one record per selected day`() = runTest {
        useCase(
            versionId = versionId,
            anchorId = 0L,
            subjectId = 5L,
            days = setOf(WeekDay.Monday, WeekDay.Wednesday, WeekDay.Friday),
            startTime = start,
            endTime = end,
            room = "101",
            teacher = "Prof"
        )

        val saved = repository.list.filter { it.subjectId == 5L }
        assertEquals(3, saved.size)
        assertEquals(
            setOf(WeekDay.Monday, WeekDay.Wednesday, WeekDay.Friday),
            saved.map { it.dayOfWeek }.toSet()
        )
        assertTrue(saved.all { it.room == "101" && it.teacherOverride == "Prof" })
        assertTrue(saved.all { it.versionId == versionId })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `create requires at least one day`() = runTest {
        useCase(
            versionId = versionId,
            anchorId = 0L,
            subjectId = 5L,
            days = emptySet(),
            startTime = start,
            endTime = end,
            room = null,
            teacher = null
        )
    }

    @Test
    fun `create trims blank room and teacher`() = runTest {
        useCase(
            versionId = versionId,
            anchorId = 0L,
            subjectId = 5L,
            days = setOf(WeekDay.Monday),
            startTime = start,
            endTime = end,
            room = "   ",
            teacher = "  "
        )

        val saved = repository.list.single()
        assertEquals(null, saved.room)
        assertEquals(null, saved.teacherOverride)
    }

    @Test
    fun `edit adds new days and keeps existing siblings`() = runTest {
        val monday = Schedule(
            id = 1L,
            subjectId = 5L,
            dayOfWeek = WeekDay.Monday,
            startTime = start,
            endTime = end,
            versionId = versionId
        )
        repository.list.add(monday)

        useCase(
            versionId = versionId,
            anchorId = 1L,
            subjectId = 5L,
            days = setOf(WeekDay.Monday, WeekDay.Thursday),
            startTime = start,
            endTime = end,
            room = "202",
            teacher = null
        )

        val saved = repository.list.filter { it.subjectId == 5L }
        assertEquals(2, saved.size)
        assertEquals(setOf(WeekDay.Monday, WeekDay.Thursday), saved.map { it.dayOfWeek }.toSet())
        assertTrue(saved.all { it.room == "202" })
    }

    @Test
    fun `edit removes siblings whose days were deselected`() = runTest {
        val monday = Schedule(
            id = 1L,
            subjectId = 5L,
            dayOfWeek = WeekDay.Monday,
            startTime = start,
            endTime = end,
            versionId = versionId
        )
        val wednesday = Schedule(
            id = 2L,
            subjectId = 5L,
            dayOfWeek = WeekDay.Wednesday,
            startTime = start,
            endTime = end,
            versionId = versionId
        )
        repository.list.addAll(listOf(monday, wednesday))

        useCase(
            versionId = versionId,
            anchorId = 1L,
            subjectId = 5L,
            days = setOf(WeekDay.Monday),
            startTime = start,
            endTime = end,
            room = null,
            teacher = null
        )

        val saved = repository.list.filter { it.subjectId == 5L }
        assertEquals(1, saved.size)
        assertEquals(WeekDay.Monday, saved.single().dayOfWeek)
    }

    @Test
    fun `edit keeps the anchor when it is still selected`() = runTest {
        val monday = Schedule(
            id = 1L,
            subjectId = 5L,
            dayOfWeek = WeekDay.Monday,
            startTime = start,
            endTime = end,
            versionId = versionId,
            room = "old"
        )
        repository.list.add(monday)

        useCase(
            versionId = versionId,
            anchorId = 1L,
            subjectId = 5L,
            days = setOf(WeekDay.Monday, WeekDay.Friday),
            startTime = start,
            endTime = end,
            room = "new",
            teacher = null
        )

        val anchor = repository.list.find { it.id == 1L }
        assertEquals(WeekDay.Monday, anchor?.dayOfWeek)
        assertEquals("new", anchor?.room)
    }
}
