package com.attendance.tracker.backend.feature.reports.routes

import com.attendance.tracker.backend.feature.reports.model.BugReportCreateRequest
import com.attendance.tracker.backend.feature.reports.model.BugReportCreateResponse
import com.attendance.tracker.backend.feature.reports.model.UploadStatus
import com.attendance.tracker.backend.feature.reports.model.toDto
import com.attendance.tracker.backend.feature.reports.service.BugReportService
import com.attendance.tracker.backend.security.SecurityAuthenticationPlugin
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.install
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import com.attendance.tracker.backend.security.repository.DeviceRepository
import com.attendance.tracker.backend.security.repository.DeviceRepositoryImpl

/**
 * Declares routes for bug report metadata submission, querying, and screenshot uploads.
 * Enforces security authentication middleware on all paths.
 */
fun Route.bugReportRoutes(
    service: BugReportService = BugReportService(),
    deviceRepository: DeviceRepository = DeviceRepositoryImpl()
) {
    route("/api/v1/reports") {
        // Apply route-scoped signature and security validations middleware
        install(SecurityAuthenticationPlugin) {
            this.deviceRepository = deviceRepository
        }

        post {
            val request = call.receive<BugReportCreateRequest>()
            val report = service.createReport(request)
            call.respond(
                HttpStatusCode.Created,
                BugReportCreateResponse(
                    reportId = report.reportId,
                    status = report.uploadStatus.name
                )
            )
        }

        get("/{reportId}") {
            val reportId = call.parameters["reportId"]
            if (reportId.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Missing required parameter 'reportId'")
                )
                return@get
            }

            val report = service.getReport(reportId)
            if (report == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Report not found for ID: $reportId")
                )
            } else {
                call.respond(HttpStatusCode.OK, report.toDto())
            }
        }

        post("/{reportId}/screenshot") {
            val reportId = call.parameters["reportId"]
            if (reportId.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Missing required parameter 'reportId'")
                )
                return@post
            }

            var fileBytes: ByteArray? = null
            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                if (part is PartData.FileItem && part.name == "screenshot") {
                    fileBytes = part.provider().readRemaining().readByteArray()
                }
                part.dispose()
            }

            if (fileBytes == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Multipart file field 'screenshot' is missing")
                )
                return@post
            }

            val objectKey = service.uploadScreenshot(reportId, fileBytes)
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "reportId" to reportId,
                    "status" to UploadStatus.SCREENSHOT_UPLOADED.name,
                    "objectKey" to objectKey
                )
            )
        }
    }
}
