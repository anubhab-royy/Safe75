package com.attendance.tracker.feature.device.data.remote

import com.attendance.tracker.feature.device.data.model.ChallengeResponse
import com.attendance.tracker.feature.device.data.model.EnrollRequest
import com.attendance.tracker.feature.device.data.model.EnrollResponse
import com.attendance.tracker.feature.device.data.model.HealthResponse
import com.attendance.tracker.feature.device.data.model.BugReportCreateRequest
import com.attendance.tracker.feature.device.data.model.BugReportCreateResponse
import com.attendance.tracker.feature.device.data.model.ScreenshotUploadResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface Safe75ApiService {

    @GET("health")
    suspend fun getHealth(): Response<HealthResponse>

    @GET("api/v1/device/challenge")
    suspend fun getChallenge(): Response<ChallengeResponse>

    @POST("api/v1/device/enroll")
    suspend fun enrollDevice(
        @Body request: EnrollRequest
    ): Response<EnrollResponse>

    @POST("api/v1/reports")
    suspend fun submitBugReport(
        @Body request: BugReportCreateRequest
    ): Response<BugReportCreateResponse>

    @Multipart
    @POST("api/v1/reports/{reportId}/screenshot")
    suspend fun uploadScreenshot(
        @Path("reportId") reportId: String,
        @Part screenshot: MultipartBody.Part
    ): Response<ScreenshotUploadResponse>
}
