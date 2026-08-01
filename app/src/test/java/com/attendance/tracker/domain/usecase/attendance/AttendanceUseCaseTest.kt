package com.attendance.tracker.domain.usecase.attendance

import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.repository.AttendanceRepository
import com.attendance.tracker.domain.validation.AttendanceValidator
import com.attendance.tracker.domain.validation.ValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class FakeAttendanceRepository : AttendanceRepository {
    val list = mutableListOf<Attendance>()

    override fun observeAttendanceForSubject(subjectId: Long): Flow<List<Attendance>> = flow {
        emit(list.filter { it.subjectId == subjectId })
    }

    override fun observeAttendanceForDate(date: LocalDate): Flow<List<Attendance>> = flow {
        emit(list.filter { it.date == date })
    }

    override fun observeTodayAttendance(date: LocalDate): Flow<List<Attendance>> = flow {
        emit(list.filter { it.date == date })
    }

    override fun observeAttendanceHistory(): Flow<List<Attendance>> = flow {
        emit(list.sortedByDescending { it.date })
    }

    override fun observeAttendanceById(id: Long): Flow<Attendance?> = flow {
        emit(list.find { it.id == id })
    }

    override suspend fun getAttendanceById(id: Long): Attendance? = list.find { it.id == id }

    override suspend fun getAttendanceForDateSync(date: LocalDate): List<Attendance> = list.filter { it.date == date }

    override suspend fun insertAttendance(attendance: Attendance): Long {
        val id = if (attendance.id == 0L) (list.size + 1).toLong() else attendance.id
        list.removeAll { it.id == id }
        list.add(attendance.copy(id = id))
        return id
    }

    override suspend fun updateAttendance(attendance: Attendance): Int {
        val idx = list.indexOfFirst { it.id == attendance.id }
        return if (idx != -1) {
            list[idx] = attendance
            1
        } else {
            0
        }
    }

    override suspend fun deleteAttendance(attendance: Attendance): Int {
        return if (list.removeIf { it.id == attendance.id }) 1 else 0
    }

    override suspend fun checkDuplicateAttendance(subjectId: Long, scheduleId: Long, date: LocalDate): Boolean {
        return list.any { it.subjectId == subjectId && it.scheduleId == scheduleId && it.date == date }
    }

    override fun countPresent(): Flow<Int> = flow { emit(list.count { it.status == AttendanceStatus.PRESENT }) }
    override fun countAbsent(): Flow<Int> = flow { emit(list.count { it.status == AttendanceStatus.ABSENT }) }
    override fun countCancelled(): Flow<Int> = flow { emit(list.count { it.status == AttendanceStatus.CANCELLED }) }
    override fun searchAttendance(query: String): Flow<List<Attendance>> = flow {
        emit(list.filter { it.remarks?.contains(query) == true })
    }
}

class AttendanceUseCaseTest {

    private lateinit var repository: FakeAttendanceRepository
    private lateinit var validator: AttendanceValidator

    private lateinit var markUseCase: MarkAttendanceUseCase
    private lateinit var updateUseCase: UpdateAttendanceUseCase
    private lateinit var deleteUseCase: DeleteAttendanceUseCase

    @Before
    fun setUp() {
        repository = FakeAttendanceRepository()
        validator = AttendanceValidator()

        markUseCase = MarkAttendanceUseCase(repository, validator)
        updateUseCase = UpdateAttendanceUseCase(repository, validator)
        deleteUseCase = DeleteAttendanceUseCase(repository)
    }

    @Test
    fun testMarkAttendance_savesWhenValid() = runTest {
        val log = Attendance(subjectId = 1L, scheduleId = 10L, date = LocalDate.now(), status = AttendanceStatus.PRESENT)
        val result = markUseCase(log)
        assertTrue(result is ValidationResult.Valid)
        assertEquals(1, repository.list.size)
        assertEquals(10L, repository.list.first().scheduleId)
    }

    @Test
    fun testMarkAttendance_returnsInvalidForDuplicate() = runTest {
        val original = Attendance(id = 1L, subjectId = 1L, scheduleId = 10L, date = LocalDate.now(), status = AttendanceStatus.PRESENT)
        repository.list.add(original)

        val duplicate = Attendance(subjectId = 1L, scheduleId = 10L, date = LocalDate.now(), status = AttendanceStatus.ABSENT)
        val result = markUseCase(duplicate)
        assertTrue(result is ValidationResult.Invalid)
        assertEquals("Attendance has already been marked for this class slot on this date", (result as ValidationResult.Invalid).reason)
    }

    @Test
    fun testDeleteAttendance_delegatesCorrectly() = runTest {
        val record = Attendance(id = 5L, subjectId = 1L, scheduleId = 10L, date = LocalDate.now(), status = AttendanceStatus.PRESENT)
        repository.list.add(record)

        val rows = deleteUseCase(record)
        assertEquals(1, rows)
        assertTrue(repository.list.isEmpty())
    }
}
