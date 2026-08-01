package com.attendance.tracker.data.repository

import com.attendance.tracker.data.local.database.entity.SubjectEntity
import com.attendance.tracker.data.local.datasource.SubjectLocalDataSource
import com.attendance.tracker.domain.model.Subject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Fake implementation of [SubjectLocalDataSource] to test Repository operations cleanly.
 */
class FakeSubjectLocalDataSource : SubjectLocalDataSource {
    val list = mutableListOf<SubjectEntity>()

    override fun observeSubjects(): Flow<List<SubjectEntity>> = flow { emit(list) }
    override suspend fun getSubjects(): List<SubjectEntity> = list
    override suspend fun getSubject(id: Long): SubjectEntity? = list.find { it.id == id }
    override suspend fun searchSubjects(query: String): List<SubjectEntity> = list.filter {
        it.name.contains(query, ignoreCase = true) || it.facultyName?.contains(query, ignoreCase = true) == true
    }
    override suspend fun countSubjects(): Int = list.size
    override suspend fun insertSubject(subject: SubjectEntity): Long {
        list.add(subject)
        return subject.id
    }
    override suspend fun updateSubject(subject: SubjectEntity): Int {
        val idx = list.indexOfFirst { it.id == subject.id }
        return if (idx != -1) {
            list[idx] = subject
            1
        } else {
            0
        }
    }
    override suspend fun deleteSubject(subject: SubjectEntity): Int {
        return if (list.removeIf { it.id == subject.id }) 1 else 0
    }
}

/**
 * Unit tests verifying [SubjectRepositoryImpl] behavior.
 */
class SubjectRepositoryImplTest {

    private lateinit var dataSource: FakeSubjectLocalDataSource
    private lateinit var repository: SubjectRepositoryImpl

    @Before
    fun setUp() {
        dataSource = FakeSubjectLocalDataSource()
        repository = SubjectRepositoryImpl(dataSource)
    }

    @Test
    fun testInsertSubject_mapsToEntityAndSaves() = runTest {
        val subject = Subject(id = 12L, name = "Data Structures")
        val id = repository.insertSubject(subject)

        assertEquals(12L, id)
        assertEquals(1, dataSource.list.size)
        assertEquals("Data Structures", dataSource.list.first().name)
    }

    @Test
    fun testGetSubjectById_mapsToDomain() = runTest {
        val entity = SubjectEntity(id = 45L, name = "Data Science")
        dataSource.list.add(entity)

        val result = repository.getSubjectById(45L)
        assertNotNull(result)
        assertEquals(45L, result?.id)
        assertEquals("Data Science", result?.name)
    }

    @Test
    fun testObserveSubjects_emitsFlowOfDomain() = runTest {
        val entity = SubjectEntity(id = 1L, name = "Economics")
        dataSource.list.add(entity)

        val list = repository.observeSubjects().first()
        assertEquals(1, list.size)
        assertEquals("Economics", list.first().name)
    }
}
