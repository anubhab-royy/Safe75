package com.attendance.tracker.backend.feature.reports.validation

import com.attendance.tracker.backend.feature.reports.model.BugReportCreateRequest

/**
 * Exception thrown when bug report validation rules are violated.
 */
class ValidationException(val errors: List<String>) : Exception("Validation failed: ${errors.joinToString(", ")}")

/**
 * Enforces business rules and validates incoming bug report metadata.
 */
object BugReportValidator {
    private val semVerRegex = Regex("""^\d+(\.\d+)*(-[a-zA-Z0-9.]+)?$""")
    private val localeRegex = Regex("""^[a-zA-Z]{2,3}([-_][a-zA-Z0-9]{2,8})*$""")

    /**
     * Validates [request]. Throws [ValidationException] if any rules are violated.
     */
    fun validate(request: BugReportCreateRequest) {
        val errors = mutableListOf<String>()

        if (request.appVersion.isBlank()) {
            errors.add("appVersion cannot be blank")
        } else if (!semVerRegex.matches(request.appVersion)) {
            errors.add("appVersion must follow semantic versioning format")
        }

        if (request.versionCode <= 0) {
            errors.add("versionCode must be greater than 0")
        }

        if (request.buildType.isBlank()) {
            errors.add("buildType cannot be blank")
        }

        if (request.androidVersion.isBlank()) {
            errors.add("androidVersion cannot be blank")
        }

        if (request.sdkVersion <= 0) {
            errors.add("sdkVersion must be greater than 0")
        }

        if (request.deviceManufacturer.isBlank()) {
            errors.add("deviceManufacturer cannot be blank")
        }

        if (request.deviceModel.isBlank()) {
            errors.add("deviceModel cannot be blank")
        }

        if (request.cpuAbi.isBlank()) {
            errors.add("cpuAbi cannot be blank")
        }

        if (request.locale.isBlank()) {
            errors.add("locale cannot be blank")
        } else if (!localeRegex.matches(request.locale)) {
            errors.add("locale must follow valid ISO language tag format")
        }

        if (request.userDescription.isBlank()) {
            errors.add("userDescription cannot be blank")
        } else if (request.userDescription.length > 1000) {
            errors.add("userDescription exceeds maximum length of 1000 characters")
        }

        if (request.diagnosticsMetadata.isBlank()) {
            errors.add("diagnosticsMetadata cannot be blank")
        }

        val maxAllowedTime = System.currentTimeMillis() + 3600000L // allow up to 1 hour future drift
        if (request.timestamp <= 0) {
            errors.add("timestamp must be positive")
        } else if (request.timestamp > maxAllowedTime) {
            errors.add("timestamp cannot be in the future")
        }

        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
    }
}
