package com.attendance.tracker.feature.bugreport.data

import com.attendance.tracker.feature.device.data.remote.NetworkError
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object BugReportRetryPolicy {
    const val MAX_RETRIES = 5

    fun isTransient(error: Throwable): Boolean = when (error) {
        is NetworkError.Timeout,
        is NetworkError.UnknownHost,
        is NetworkError.Io,
        is NetworkError.ServerError -> true
        is SocketTimeoutException,
        is UnknownHostException,
        is IOException -> true
        else -> false
    }

    fun safeReason(error: Throwable): String = when (error) {
        is NetworkError.Timeout, is SocketTimeoutException -> "Request timed out"
        is NetworkError.UnknownHost, is UnknownHostException -> "Network unavailable"
        is NetworkError.Io, is IOException -> "Network I/O failure"
        is NetworkError.ServerError -> "Backend temporarily unavailable"
        is NetworkError.Unauthorized -> "Authentication failed"
        is NetworkError.Forbidden -> "Access denied"
        is NetworkError.Validation -> "Backend validation failed"
        is NetworkError.NotFound -> "Report not found"
        is NetworkError.Conflict -> "Duplicate report rejected"
        is NetworkError.MalformedResponse, is NetworkError.Serialization -> "Invalid backend response"
        else -> "Upload failed"
    }
}
