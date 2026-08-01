package com.attendance.tracker.domain.usecase.planner

import com.attendance.tracker.domain.model.SimulationResult
import javax.inject.Inject

/**
 * Domain Use Case to run attendance simulations in memory without updating the database.
 */
class AttendanceSimulatorUseCase @Inject constructor() {

    operator fun invoke(
        currentPresent: Int,
        currentAbsent: Int,
        simulatePresent: Int,
        simulateAbsent: Int
    ): SimulationResult {
        val currentTotal = currentPresent + currentAbsent
        val currentPct = if (currentTotal > 0) {
            (currentPresent.toDouble() / currentTotal.toDouble()) * 100.0
        } else {
            100.0
        }

        val simulatedPresent = currentPresent + simulatePresent
        val simulatedAbsent = currentAbsent + simulateAbsent
        val simulatedTotal = simulatedPresent + simulatedAbsent

        val simulatedPct = if (simulatedTotal > 0) {
            (simulatedPresent.toDouble() / simulatedTotal.toDouble()) * 100.0
        } else {
            100.0
        }

        val difference = simulatedPct - currentPct

        return SimulationResult(
            currentPercentage = currentPct,
            simulatedPercentage = simulatedPct,
            difference = difference
        )
    }
}
