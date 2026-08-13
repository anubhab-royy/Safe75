package com.attendance.tracker.backend

import com.attendance.tracker.backend.feature.reports.routes.bugReportRoutes
import com.attendance.tracker.backend.feature.reports.service.BugReportService
import com.attendance.tracker.backend.feature.reports.validation.ValidationException
import com.attendance.tracker.backend.routes.healthRoute
import com.attendance.tracker.backend.security.repository.DeviceRepositoryImpl
import com.attendance.tracker.backend.security.routes.securityRoutes
import com.mongodb.MongoWriteException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.doublereceive.DoubleReceive
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

/**
 * Entry point for the Ktor backend application.
 *
 * Uses EngineMain to start the server configured in application.conf.
 */
fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

/**
 * Main application module called by the Ktor engine.
 */
fun Application.module() {
    val bugReportService = BugReportService()
    val deviceRepository = DeviceRepositoryImpl()

    // Launch DB index creations asynchronously on startup
    launch {
        bugReportService.initializeDb()
        deviceRepository.createIndexes()
    }

    // Install DoubleReceive to allow reading request body in signature check middleware
    install(DoubleReceive)

    // Setup Content Negotiation with Kotlin Serialization
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    // Logging framework hook
    install(CallLogging) {
        level = Level.INFO
    }

    // Centralized Exception Handling
    install(StatusPages) {
        // Handle validation errors from input fields
        exception<ValidationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("errors" to cause.errors)
            )
        }

        // Handle client validation errors
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to (cause.message ?: "Invalid request argument"))
            )
        }

        // Handle resource not found errors
        exception<NoSuchElementException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to (cause.message ?: "Resource not found"))
            )
        }

        // Handle state conflict errors (e.g. duplicate upload)
        exception<IllegalStateException> { call, cause ->
            call.respond(
                HttpStatusCode.Conflict,
                mapOf("error" to (cause.message ?: "Resource conflict state"))
            )
        }

        // Handle database duplicate key conflicts
        exception<MongoWriteException> { call, cause ->
            if (cause.error.code == 11000) {
                call.respond(
                    HttpStatusCode.Conflict,
                    mapOf("error" to "A record with this identifier already exists")
                )
            } else {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Database operation failed")
                )
            }
        }

        // Catch-all for unexpected failures
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf("error" to (cause.message ?: "An unexpected internal server error occurred"))
            )
        }
    }

    // Routes setup
    routing {
        healthRoute()
        securityRoutes(deviceRepository)
        bugReportRoutes(bugReportService)
    }
}
