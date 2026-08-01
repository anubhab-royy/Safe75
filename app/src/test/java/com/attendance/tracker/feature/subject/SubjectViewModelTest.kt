package com.attendance.tracker.feature.subject

import com.attendance.tracker.core.common.UiState
import com.attendance.tracker.domain.usecase.subject.AddSubjectUseCase
import com.attendance.tracker.domain.usecase.subject.DeleteSubjectUseCase
import com.attendance.tracker.domain.usecase.subject.FakeSubjectRepository
import com.attendance.tracker.domain.usecase.subject.GetSubjectUseCase
import com.attendance.tracker.domain.usecase.subject.GetSubjectsUseCase
import com.attendance.tracker.domain.usecase.subject.ObserveSubjectsUseCase
import com.attendance.tracker.domain.usecase.subject.UpdateSubjectUseCase
import com.attendance.tracker.domain.validation.SubjectValidator
import com.attendance.tracker.domain.validation.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying view model reactive flows in [SubjectViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SubjectViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: FakeSubjectRepository
    private lateinit var viewModel: SubjectViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeSubjectRepository()

        val observeUseCase = ObserveSubjectsUseCase(repository)
        val addUseCase = AddSubjectUseCase(repository)
        val updateUseCase = UpdateSubjectUseCase(repository)
        val deleteUseCase = DeleteSubjectUseCase(repository)
        val getSubjectUseCase = GetSubjectUseCase(repository)
        val getSubjectsUseCase = GetSubjectsUseCase(repository)
        val validator = SubjectValidator()

        viewModel = SubjectViewModel(
            observeSubjectsUseCase = observeUseCase,
            addSubjectUseCase = addUseCase,
            updateSubjectUseCase = updateUseCase,
            deleteSubjectUseCase = deleteUseCase,
            getSubjectUseCase = getSubjectUseCase,
            getSubjectsUseCase = getSubjectsUseCase,
            validator = validator
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState_isLoading() = runTest {
        assertTrue(viewModel.subjectsState.value is UiState.Loading)
    }

    @Test
    fun testLoadSubjectsEmpty_returnsEmptyState() = runTest {
        // Let flow combination trigger
        advanceUntilIdle()
        assertTrue(viewModel.subjectsState.value is UiState.Empty)
    }

    @Test
    fun testSearchFilter_filtersSubjectsList() = runTest {
        val s1 = com.attendance.tracker.domain.model.Subject(id = 1L, name = "Software Eng")
        val s2 = com.attendance.tracker.domain.model.Subject(id = 2L, name = "Calculus")
        repository.subjects.addAll(listOf(s1, s2))

        // Trigger updates
        viewModel.onSearchQueryChange("Software")
        advanceUntilIdle()

        val state = viewModel.subjectsState.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals(1, data.size)
        assertEquals("Software Eng", data.first().name)
    }

    @Test
    fun testSortOrderAlphabetical_sortsCorrectly() = runTest {
        val s1 = com.attendance.tracker.domain.model.Subject(id = 1L, name = "Physics")
        val s2 = com.attendance.tracker.domain.model.Subject(id = 2L, name = "Chemistry")
        repository.subjects.addAll(listOf(s1, s2))

        viewModel.onSortOptionChange(SubjectSortOption.ALPHABETICAL)
        advanceUntilIdle()

        val state = viewModel.subjectsState.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals("Chemistry", data[0].name)
        assertEquals("Physics", data[1].name)
    }

    @Test
    fun testSaveInvalidSubject_returnsFalseAndSetsValidationError() = runTest {
        advanceUntilIdle()
        val success = viewModel.saveSubject(
            name = "", // Invalid: Empty name
            faculty = "Prof",
            color = 0,
            required = 75,
            goal = 85
        )
        advanceUntilIdle()
        assertTrue(!success)
        assertTrue(viewModel.validationState.value is ValidationResult.Invalid)
        assertEquals("Subject name is required", (viewModel.validationState.value as ValidationResult.Invalid).reason)
    }

    @Test
    fun testSaveValidSubject_savesCorrectly() = runTest {
        advanceUntilIdle()
        val success = viewModel.saveSubject(
            name = "Microbiology",
            faculty = "Dr. Pasteur",
            color = 12345,
            required = 75,
            goal = 85
        )
        advanceUntilIdle()
        assertTrue(success)
        assertEquals(1, repository.subjects.size)
        assertEquals("Microbiology", repository.subjects.first().name)
    }

    @Test
    fun testDeleteSubject_removesItem() = runTest {
        val subject = com.attendance.tracker.domain.model.Subject(id = 100L, name = "History")
        repository.subjects.add(subject)
        advanceUntilIdle()

        viewModel.deleteSubject(100L)
        advanceUntilIdle()

        assertTrue(repository.subjects.isEmpty())
    }
}
