package com.attendance.tracker.feature.device.data.remote

import retrofit2.Response

object ErrorHandler {

    fun <T> handleResponse(response: Response<T>): T {
        if (response.isSuccessful) {
            return response.body()
                ?: throw NetworkError.MalformedResponse("Response body is null for status ${response.code()}")
        }

        val errorBody = response.errorBody()?.string()
        val message = extractErrorMessage(errorBody)

        when (response.code()) {
            400 -> throw NetworkError.Validation(message)
            401 -> throw NetworkError.Unauthorized(message)
            403 -> throw NetworkError.Forbidden(message)
            404 -> throw NetworkError.NotFound(message)
            409 -> throw NetworkError.Conflict(message)
            422 -> throw NetworkError.Validation(message)
            429 -> throw NetworkError.RateLimited(message)
            500, 502, 503, 504 -> throw NetworkError.ServerError(message)
            else -> throw NetworkError.ServerError("HTTP ${response.code()}: $message")
        }
    }

    private fun extractErrorMessage(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) {
            return "No error details provided by server"
        }
        return errorBody.trim()
    }
}
