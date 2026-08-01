package com.attendance.tracker.domain.usecase.subject

import com.attendance.tracker.domain.model.Subject
import com.attendance.tracker.domain.repository.SubjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Fake implementation of [SubjectRepository] for lightweight JVM unit testing.
 */
class FakeSubjectRepository : SubjectRepository {
    val subjects = mutableListOf<Subject>()

    override fun observeSubjects(): Flow<List<Subject>> = flow {
        emit(subjects)
    }

    override suspend fun getSubjects(): List<Subject> = subjects

    override suspend fun getSubjectById(id: Long): Subject? = subjects.find { it.id == id }

    override suspend fun insertSubject(subject: Subject): Long {
        val newId = if (subject.id == 0L) (subjects.size + 1).toLong() else subject.id
        subjects.removeAll { it.id == newId }
        val finalSubject = subject.copy(id = newId)
        subjects.add(finalSubject)
        return newId
    }

    override suspend fun updateSubject(subject: Subject): Int {
        val index = subjects.indexOfFirst { it.id == subject.id }
        return if (index != -1) {
            subjects[index] = subject
            1
        } else {
            0
        }
    }

    override suspend fun deleteSubject(subject: Subject): Int {
        return if (subjects.removeIf { it.id == subject.id }) 1 else 0
    }

    override suspend fun searchSubjects(query: String): List<Subject> {
        return subjects.filter {
            it.name.contains(query, ignoreCase = true) ||
                    (it.facultyName?.contains(query, ignoreCase = true) == true)
        }
    }

    override suspend fun countSubjects(): Int = subjects.size
}

/**
 * Unit tests for Subject Use Cases.
 */
class SubjectUseCaseTest {

    private lateinit var repository: FakeSubjectRepository
    private lateinit var addUseCase: AddSubjectUseCase
    private lateinit var updateUseCase: UpdateSubjectUseCase
    private lateinit var deleteUseCase: DeleteSubjectUseCase
    private lateinit var getSubjectUseCase: GetSubjectUseCase
    private lateinit var getSubjectsUseCase: GetSubjectsUseCase
    private lateinit var observeSubjectsUseCase: ObserveSubjectsUseCase
    private lateinit var searchSubjectsUseCase: SearchSubjectsUseCase

    @Before
    fun setUp() {
        repository = FakeSubjectRepository()
        addUseCase = AddSubjectUseCase(repository)
        updateUseCase = UpdateSubjectUseCase(repository)
        deleteUseCase = DeleteSubjectUseCase(repository)
        getSubjectUseCase = GetSubjectUseCase(repository)
        getSubjectsUseCase = GetSubjectsUseCase(repository)
        observeSubjectsUseCase = ObserveSubjectsUseCase(repository)
        searchSubjectsUseCase = SearchSubjectsUseCase(repository)
    }

    @Test
    fun testAddSubject_savesSubject() = runTest {
        val subject = Subject(name = "Biology")
        val id = addUseCase(subject)
        
        assertEquals(1L, id)
        assertEquals(1, repository.subjects.size)
        assertEquals("Biology", repository.subjects.first().name)
    }

    @Test
    fun testUpdateSubject_modifiesSubject() = runTest {
        val original = Subject(id = 5L, name = "Original Chemistry")
        repository.subjects.add(original)

        val updated = original.copy(name = "Organic Chemistry")
        val count = updateUseCase(updated)

        assertEquals(1, count)
        assertEquals("Organic Chemistry", repository.subjects.first().name)
    }

    @Test
    fun testDeleteSubject_removesSubject() = runTest {
        val original = Subject(id = 3L, name = "Physics")
        repository.subjects.add(original)

        val count = deleteUseCase(original)
        assertEquals(1, count)
        assertTrue(repository.subjects.isEmpty())
    }

    @Test
    fun testGetSubject_returnsCorrectSubject() = runTest {
        val s1 = Subject(id = 10L, name = "Math")
        val s2 = Subject(id = 11L, name = "History")
        repository.subjects.addAll(listOf(s1, s2))

        val fetched = getSubjectUseCase(11L)
        assertNotNull(fetched)
        assertEquals("History", fetched?.name)

        val nonExistent = getSubjectUseCase(99L)
        assertNull(nonExistent)
    }

    @Test
    fun testGetAndObserveSubjects_returnsLists() = runTest {
        val s = Subject(name = "English")
        repository.subjects.add(s)

        val list = getSubjectsUseCase()
        assertEquals(1, list.size)

        val observed = observeSubjectsUseCase().first()
        assertEquals(1, observed.size)
    }

    @Test
    fun testSearchSubjects_filtersProperly() = runTest {
        val s1 = Subject(name = "Calculus", facultyName = "Dr. Einstein")
        val s2 = Subject(name = "Algebra", facultyName = "Prof. Newton")
        repository.subjects.addAll(listOf(s1, s2))

        val resultsName = searchSubjectsUseCase("Calc")
        assertEquals(1, resultsName.size)
        assertEquals("Calculus", resultsName.first().name)

        val resultsFaculty = searchSubjectsUseCase("Newton")
        assertEquals(1, resultsFaculty.size)
        assertEquals("Algebra", resultsFaculty.first().name)
    }
}
