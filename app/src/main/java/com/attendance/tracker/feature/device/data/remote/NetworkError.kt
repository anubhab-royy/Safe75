package com.attendance.tracker.feature.device.data.remote

import kotlinx.serialization.SerializationException

sealed class NetworkError(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    class Timeout(message: String = "Request timed out", cause: Throwable? = null) :
        NetworkError(message, cause)

    class UnknownHost(message: String = "Unable to resolve host: device is offline or DNS failed", cause: Throwable? = null) :
        NetworkError(message, cause)

    class SslHandshake(message: String = "SSL/TLS handshake failed", cause: Throwable? = null) :
        NetworkError(message, cause)

    class Unauthorized(message: String = "Authentication failed: invalid credentials or signature", cause: Throwable? = null) :
        NetworkError(message, cause)

    class EnrollmentMissing(message: String = "Device enrollment is required", cause: Throwable? = null) :
        NetworkError(message, cause)

    class Validation(message: String = "The report could not be validated", cause: Throwable? = null) :
        NetworkError(message, cause)

    class NotFound(message: String = "The requested report was not found", cause: Throwable? = null) :
        NetworkError(message, cause)

    class Conflict(message: String = "The report conflicts with an existing upload", cause: Throwable? = null) :
        NetworkError(message, cause)

    class Forbidden(message: String = "Access denied for this resource", cause: Throwable? = null) :
        NetworkError(message, cause)

    class RateLimited(message: String = "Rate limit exceeded. Please wait before retrying.", cause: Throwable? = null) :
        NetworkError(message, cause)

    class ServerError(message: String = "Server error occurred", cause: Throwable? = null) :
        NetworkError(message, cause)

    class MalformedResponse(message: String = "Malformed or unexpected response from server", cause: Throwable? = null) :
        NetworkError(message, cause)

    class Serialization(message: String = "Failed to parse response body", cause: Throwable? = null) :
        NetworkError(message, cause)

    class Io(message: String = "Network I/O error", cause: Throwable? = null) :
        NetworkError(message, cause)

    companion object {
        fun from(cause: Throwable): NetworkError {
            val msg = cause.message ?: cause::class.java.simpleName
            return when (cause) {
                is java.net.SocketTimeoutException -> Timeout(msg, cause)
                is java.net.UnknownHostException -> UnknownHost(msg, cause)
                is javax.net.ssl.SSLHandshakeException -> SslHandshake(msg, cause)
                is SerializationException -> Serialization(msg, cause)
                is java.io.IOException -> Io(msg, cause)
                else -> Io(msg, cause)
            }
        }
    }
}
