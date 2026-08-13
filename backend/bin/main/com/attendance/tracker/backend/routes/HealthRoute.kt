package com.attendance.tracker.backend.routes

import com.attendance.tracker.backend.database.MongoClientProvider
import com.attendance.tracker.backend.storage.R2ClientProvider
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable
import java.lang.management.ManagementFactory

@Serializable
data class HealthResponse(
    val status: String,
    val version: String,
    val uptimeMs: Long,
    val database: String,
    val storage: String
)

/**
 * Defines the `/health` endpoint route.
 */
fun Route.healthRoute() {
    get("/health") {
        val uptimeMs = ManagementFactory.getRuntimeMXBean().uptime
        val isDbConnected = MongoClientProvider.checkConnection()
        val isR2Connected = R2ClientProvider.checkConnection()

        val overallStatus = if (isDbConnected && isR2Connected) "UP" else "DEGRADED"
        val response = HealthResponse(
            status = overallStatus,
            version = "1.0.0",
            uptimeMs = uptimeMs,
            database = if (isDbConnected) "CONNECTED" else "DISCONNECTED",
            storage = if (isR2Connected) "CONNECTED" else "DISCONNECTED"
        )

        val statusCode = if (overallStatus == "UP") HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
        call.respond(statusCode, response)
    }
}
