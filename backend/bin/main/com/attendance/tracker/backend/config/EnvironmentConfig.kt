package com.attendance.tracker.backend.config

import io.github.cdimascio.dotenv.Dotenv
import java.io.File

/**
 * Loads configuration values from environment variables or a local `.env` file.
 */
object EnvironmentConfig {
    private val dotenv: Dotenv? by lazy {
        try {
            if (File(".env").exists()) {
                Dotenv.configure().ignoreIfMalformed().load()
            } else if (File("../.env").exists()) {
                Dotenv.configure().directory("../").ignoreIfMalformed().load()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Retrieves the configuration value for the given [key].
     *
     * Order of precedence:
     * 1. System environment variables (standard in production hosting)
     * 2. Local `.env` file
     * 3. [defaultValue] if provided
     *
     * Throws [IllegalStateException] if the variable is missing and no default is provided.
     */
    fun get(key: String, defaultValue: String? = null): String {
        return System.getenv(key)
            ?: dotenv?.get(key)
            ?: defaultValue
            ?: throw IllegalStateException("Required environment variable '$key' is missing.")
    }
}
