package com.attendance.tracker.feature.device.data.repository

import com.attendance.tracker.feature.device.data.model.EnrollRequest
import com.attendance.tracker.feature.device.data.model.BugReportCreateRequest
import com.attendance.tracker.feature.device.data.remote.ErrorHandler
import com.attendance.tracker.feature.device.data.remote.NetworkError
import com.attendance.tracker.feature.device.data.remote.Safe75ApiService
import com.attendance.tracker.feature.device.data.security.HmacSigner
import com.attendance.tracker.feature.device.data.security.PoWSolver
import com.attendance.tracker.feature.device.data.security.SecureDeviceStorage
import com.attendance.tracker.feature.device.domain.model.EnrolledDevice
import com.attendance.tracker.feature.device.domain.model.BugReportSubmission
import com.attendance.tracker.feature.device.domain.repository.BackendRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

class BackendRepositoryImpl @Inject constructor(
    private val apiService: Safe75ApiService,
    private val secureStorage: SecureDeviceStorage,
    private val powSolver: PoWSolver,
    private val hmacSigner: HmacSigner
) : BackendRepository {

    override suspend fun checkHealth(): Result<Unit> {
        return try {
            val response = apiService.getHealth()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.status == "UP") {
                    Result.success(Unit)
                } else {
                    Result.failure(NetworkError.ServerError("Backend health check returned non-UP status: ${body?.status}"))
                }
            } else {
                Result.failure(NetworkError.from(Exception("Health check HTTP ${response.code()}")))
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(NetworkError.Timeout(e.message ?: "Timeout", e))
        } catch (e: UnknownHostException) {
            Result.failure(NetworkError.UnknownHost(e.message ?: "Unknown host", e))
        } catch (e: Exception) {
            Result.failure(NetworkError.from(e))
        }
    }

    override suspend fun requestChallenge(): Result<Pair<String, Int>> {
        return try {
            val response = apiService.getChallenge()
            val body = ErrorHandler.handleResponse(response)
            Result.success(Pair(body.seed, body.difficulty))
        } catch (e: SocketTimeoutException) {
            Result.failure(NetworkError.Timeout(e.message ?: "Timeout", e))
        } catch (e: UnknownHostException) {
            Result.failure(NetworkError.UnknownHost(e.message ?: "Unknown host", e))
        } catch (e: NetworkError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(NetworkError.from(e))
        }
    }

    override suspend fun enrollDevice(): Result<EnrolledDevice> {
        return try {
            val (seed, difficulty) = requestChallenge().getOrElse {
                return Result.failure(it)
            }

            val nonce = powSolver.solve(seed, difficulty)

            val enrollResponse = apiService.enrollDevice(
                request = EnrollRequest(
                    seed = seed,
                    nonce = nonce
                )
            )

            val enrollBody = ErrorHandler.handleResponse(enrollResponse)

            val device = EnrolledDevice(
                deviceId = enrollBody.deviceId,
                deviceSecret = enrollBody.deviceSecret,
                createdAt = enrollBody.createdAt
            )

            secureStorage.saveDevice(
                deviceId = device.deviceId,
                deviceSecret = device.deviceSecret,
                createdAt = device.createdAt
            )

            Result.success(device)
        } catch (e: SocketTimeoutException) {
            Result.failure(NetworkError.Timeout(e.message ?: "Timeout", e))
        } catch (e: UnknownHostException) {
            Result.failure(NetworkError.UnknownHost(e.message ?: "Unknown host", e))
        } catch (e: NetworkError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(NetworkError.from(e))
        }
    }

    override suspend fun getEnrolledDevice(): EnrolledDevice? {
        return secureStorage.getDevice()
    }

    override suspend fun clearEnrollment() {
        secureStorage.clearDevice()
    }

    override suspend fun submitBugReport(request: BugReportSubmission): Result<String> {
        return try {
            if (secureStorage.getDevice() == null) {
                return Result.failure(NetworkError.EnrollmentMissing())
            }
            val response = apiService.submitBugReport(
                BugReportCreateRequest(
                    timestamp = request.timestamp,
                    appVersion = request.appVersion,
                    versionCode = request.versionCode,
                    buildType = request.buildType,
                    androidVersion = request.androidVersion,
                    sdkVersion = request.sdkVersion,
                    deviceManufacturer = request.deviceManufacturer,
                    deviceModel = request.deviceModel,
                    cpuAbi = request.cpuAbi,
                    locale = request.locale,
                    crashReportId = request.crashReportId,
                    userDescription = request.userDescription,
                    diagnosticsMetadata = request.diagnosticsMetadata
                )
            )
            Result.success(ErrorHandler.handleResponse(response).reportId)
        } catch (e: SocketTimeoutException) {
            Result.failure(NetworkError.Timeout(cause = e))
        } catch (e: UnknownHostException) {
            Result.failure(NetworkError.UnknownHost(cause = e))
        } catch (e: NetworkError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(NetworkError.from(e))
        }
    }

    override suspend fun uploadScreenshot(
        reportId: String,
        imageBytes: ByteArray,
        contentType: String
    ): Result<Unit> {
        return try {
            if (secureStorage.getDevice() == null) {
                return Result.failure(NetworkError.EnrollmentMissing())
            }
            val body = imageBytes.toRequestBody(contentType.toMediaType())
            val part = MultipartBody.Part.createFormData("screenshot", "screenshot", body)
            ErrorHandler.handleResponse(apiService.uploadScreenshot(reportId, part))
            Result.success(Unit)
        } catch (e: SocketTimeoutException) {
            Result.failure(NetworkError.Timeout(cause = e))
        } catch (e: UnknownHostException) {
            Result.failure(NetworkError.UnknownHost(cause = e))
        } catch (e: NetworkError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(NetworkError.from(e))
        }
    }
}
