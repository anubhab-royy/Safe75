package com.attendance.tracker.feature.splash

import com.attendance.tracker.domain.model.SemesterVersion
import com.attendance.tracker.domain.repository.SemesterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var semesterRepository: SemesterRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        semesterRepository = mock(SemesterRepository::class.java)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState_startsInLoading() {
        `when`(semesterRepository.observeActiveVersion()).thenReturn(flowOf(null))
        val viewModel = SplashViewModel(semesterRepository)
        assertEquals(SplashDestination.Loading, viewModel.destination.value)
    }

    @Test
    fun testCheckStartupDestination_whenActiveSemesterExists_navigatesToMainGraph() = runTest {
        val activeSemester = SemesterVersion(
            id = 1L,
            name = "Semester 1",
            isActive = true,
            startDate = LocalDate.of(2026, 1, 1),
            endDate = LocalDate.of(2026, 5, 31)
        )
        `when`(semesterRepository.observeActiveVersion()).thenReturn(flowOf(activeSemester))

        val viewModel = SplashViewModel(semesterRepository)
        advanceUntilIdle()

        assertEquals(SplashDestination.MainGraph, viewModel.destination.value)
    }

    @Test
    fun testCheckStartupDestination_whenNoActiveSemester_navigatesToWelcome() = runTest {
        `when`(semesterRepository.observeActiveVersion()).thenReturn(flowOf(null))

        val viewModel = SplashViewModel(semesterRepository)
        advanceUntilIdle()

        assertEquals(SplashDestination.Welcome, viewModel.destination.value)
    }
}
