package com.attendance.tracker.feature.device.data.remote

import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class ErrorHandlerTest {

    @Test
    fun testHandleResponse_success_returnsBody() {
        val dto = TestDto("success_data")
        val response = Response.success(dto)
        val result = ErrorHandler.handleResponse(response)
        assertEquals("success_data", result.field)
    }

    @Test(expected = NetworkError.Unauthorized::class)
    fun testHandleResponse_401_throwsUnauthorized() {
        val response = Response.error<TestDto>(
            401,
            "{\"error\":\"Invalid credentials\"}".toResponseBody()
        )
        ErrorHandler.handleResponse(response)
    }

    @Test(expected = NetworkError.Forbidden::class)
    fun testHandleResponse_403_throwsForbidden() {
        val response = Response.error<TestDto>(
            403,
            "{\"error\":\"Access denied\"}".toResponseBody()
        )
        ErrorHandler.handleResponse(response)
    }

    @Test(expected = NetworkError.RateLimited::class)
    fun testHandleResponse_429_throwsRateLimited() {
        val response = Response.error<TestDto>(
            429,
            "{\"error\":\"Rate limit exceeded\"}".toResponseBody()
        )
        ErrorHandler.handleResponse(response)
    }

    @Test(expected = NetworkError.ServerError::class)
    fun testHandleResponse_500_throwsServerError() {
        val response = Response.error<TestDto>(
            500,
            "{\"error\":\"Internal Server Error\"}".toResponseBody()
        )
        ErrorHandler.handleResponse(response)
    }

    @Test(expected = NetworkError.ServerError::class)
    fun testHandleResponse_503_throwsServerError() {
        val response = Response.error<TestDto>(
            503,
            "{\"error\":\"Service Unavailable\"}".toResponseBody()
        )
        ErrorHandler.handleResponse(response)
    }

    @Test(expected = NetworkError.MalformedResponse::class)
    fun testHandleResponse_nullBody_throwsMalformed() {
        val response = Response.success<TestDto>(200, null)
        ErrorHandler.handleResponse(response)
    }

    private data class TestDto(val field: String)
}
