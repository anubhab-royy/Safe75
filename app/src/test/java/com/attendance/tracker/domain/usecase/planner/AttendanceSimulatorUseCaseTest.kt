package com.attendance.tracker.domain.usecase.planner

import org.junit.Assert.assertEquals
import org.junit.Test

class AttendanceSimulatorUseCaseTest {

    private val simulator = AttendanceSimulatorUseCase()

    @Test
    fun testSimulation_attendingClasses_increasesPercentage() {
        // Current: 2 Present, 2 Absent = 50%
        // Simulate: Attend next 2 classes (Present+2, Absent+0) -> 4 Present, 2 Absent = 66.6%
        val result = simulator(
            currentPresent = 2,
            currentAbsent = 2,
            simulatePresent = 2,
            simulateAbsent = 0
        )

        assertEquals(50.0, result.currentPercentage, 0.1)
        assertEquals(66.6, result.simulatedPercentage, 0.1)
        assertEquals(16.6, result.difference, 0.1)
    }

    @Test
    fun testSimulation_missingClasses_decreasesPercentage() {
        // Current: 3 Present, 1 Absent = 75%
        // Simulate: Bunk next 2 classes (Present+0, Absent+2) -> 3 Present, 3 Absent = 50%
        val result = simulator(
            currentPresent = 3,
            currentAbsent = 1,
            simulatePresent = 0,
            simulateAbsent = 2
        )

        assertEquals(75.0, result.currentPercentage, 0.1)
        assertEquals(50.0, result.simulatedPercentage, 0.1)
        assertEquals(-25.0, result.difference, 0.1)
    }
}
