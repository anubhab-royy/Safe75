package com.attendance.tracker.core.backup

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Converts a raw JSON string into a typed [BackupData] object.
 *
 * Returns [Result.success] on valid JSON, [Result.failure] on any parse or
 * schema error, so callers can handle errors without catching exceptions.
 */
@Singleton
class BackupDeserializer @Inject constructor(
    private val json: Json
) {

    /**
     * Attempts to parse a JSON string into [BackupData].
     *
     * @param jsonString Raw JSON content from a backup file.
     * @return [Result.success] wrapping the parsed [BackupData], or
     *         [Result.failure] wrapping a [SerializationException] / [IllegalArgumentException].
     */
    fun deserialize(jsonString: String): Result<BackupData> {
        return try {
            if (jsonString.isBlank()) {
                Result.failure(IllegalArgumentException("Backup file is empty."))
            } else {
                Result.success(json.decodeFromString<BackupData>(jsonString))
            }
        } catch (e: SerializationException) {
            Result.failure(IllegalArgumentException("Invalid backup format: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parses an embedded subjects JSON string (from an archive entity) into a list of DTOs.
     */
    fun deserializeSubjects(jsonString: String): List<BackupSubjectDto> {
        return try {
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Parses an embedded schedules JSON string (from an archive entity) into a list of DTOs.
     */
    fun deserializeSchedules(jsonString: String): List<BackupScheduleDto> {
        return try {
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Parses an embedded attendance JSON string (from an archive entity) into a list of DTOs.
     */
    fun deserializeAttendance(jsonString: String): List<BackupAttendanceDto> {
        return try {
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
