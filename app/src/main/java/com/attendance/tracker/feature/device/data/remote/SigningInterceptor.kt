package com.attendance.tracker.feature.device.data.remote

import com.attendance.tracker.feature.device.data.security.HmacSigner
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import okio.IOException

import com.attendance.tracker.feature.device.data.security.SecureDeviceStorage

class SigningInterceptor(
    private val secureStorage: SecureDeviceStorage
) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val device = secureStorage.getDevice()

        if (device == null) {
            return chain.proceed(original)
        }

        val deviceId = device.deviceId
        val deviceSecret = device.deviceSecret

        if (deviceId.isBlank() || deviceSecret.isBlank()) {
            return chain.proceed(original)
        }

        val method = original.method

        val bodyToSign = if (method == "POST" || method == "PUT" || method == "PATCH") {
            val body = original.body
            val contentType = body?.contentType()
            val isMultipart = contentType != null && contentType.type == "multipart"
            if (body != null && !isMultipart) {
                val buffer = Buffer()
                body.writeTo(buffer)
                buffer.readUtf8()
            } else {
                ""
            }
        } else {
            ""
        }

        val nonce = HmacSigner.generateNonce()
        val timestamp = System.currentTimeMillis()
        val dataToSign = "$bodyToSign$nonce$timestamp"
        val signature = HmacSigner.sign(dataToSign, deviceSecret)

        val requestBuilder = original.newBuilder()
            .addHeader("X-Safe75-DeviceId", deviceId)
            .addHeader("X-Safe75-Signature", signature)
            .addHeader("X-Safe75-Nonce", nonce)
            .addHeader("X-Safe75-Timestamp", timestamp.toString())

        return chain.proceed(requestBuilder.build())
    }
}
