package com.attendance.tracker.backend.security.routes

import com.attendance.tracker.backend.security.ChallengeManager
import com.attendance.tracker.backend.security.model.ChallengeResponse
import com.attendance.tracker.backend.security.model.Device
import com.attendance.tracker.backend.security.model.DeviceStatus
import com.attendance.tracker.backend.security.model.EnrollRequest
import com.attendance.tracker.backend.security.model.EnrollResponse
import com.attendance.tracker.backend.security.repository.DeviceRepository
import com.attendance.tracker.backend.security.repository.DeviceRepositoryImpl
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.security.SecureRandom
import java.util.UUID

/**
 * Defines routes for Proof-of-Work challenge distribution and dynamic device enrollment.
 */
fun Route.securityRoutes(repository: DeviceRepository = DeviceRepositoryImpl()) {
    route("/api/v1/device") {
        get("/challenge") {
            val seed = ChallengeManager.generateChallenge()
            call.respond(
                HttpStatusCode.OK,
                ChallengeResponse(
                    seed = seed,
                    difficulty = ChallengeManager.difficulty
                )
            )
        }

        post("/enroll") {
            val request = call.receive<EnrollRequest>()

            val isValid = ChallengeManager.verifyChallenge(request.seed, request.nonce)
            if (!isValid) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid or expired Proof-of-Work challenge solution")
                )
                return@post
            }

            val deviceId = "dev_${UUID.randomUUID()}"

            // Generate 32 bytes cryptographically secure random deviceSecret key in hex format
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            val deviceSecret = "sec_${bytes.joinToString("") { "%02x".format(it) }}"

            val currentTime = System.currentTimeMillis()
            val device = Device(
                deviceId = deviceId,
                deviceSecret = deviceSecret,
                status = DeviceStatus.ACTIVE,
                createdAt = currentTime,
                lastSeen = currentTime
            )

            val saved = repository.save(device)
            if (!saved) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to persist device enrollment in database")
                )
                return@post
            }

            call.respond(
                HttpStatusCode.Created,
                EnrollResponse(
                    deviceId = deviceId,
                    deviceSecret = deviceSecret,
                    createdAt = currentTime
                )
            )
        }
    }
}
