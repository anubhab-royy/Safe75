package com.attendance.tracker.domain.model

/**
 * Domain model representing in-memory simulator comparisons.
 */
data class SimulationResult(
    val currentPercentage: Double,
    val simulatedPercentage: Double,
    val difference: Double
)
