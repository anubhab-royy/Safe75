package com.attendance.tracker.core.backup

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Converts a [BackupData] domain payload into a pretty-printed JSON string
 * ready for writing to a file.
 *
 * Uses [kotlinx.serialization] for consistent serialization across the app.
 */
@Singleton
class BackupSerializer @Inject constructor() {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    /**
     * Encodes a [BackupData] object into a formatted JSON string.
     *
     * @param data The backup payload to serialize.
     * @return JSON string representation of the backup.
     */
    fun serialize(data: BackupData): String {
        return json.encodeToString(data)
    }

    /**
     * Encodes a list of [BackupSubjectDto] to a compact JSON string.
     * Used for embedding subject snapshots inside [com.attendance.tracker.data.local.database.entity.ArchiveEntity].
     */
    fun serializeSubjects(subjects: List<BackupSubjectDto>): String {
        return json.encodeToString(subjects)
    }

    /**
     * Encodes a list of [BackupScheduleDto] to a compact JSON string.
     */
    fun serializeSchedules(schedules: List<BackupScheduleDto>): String {
        return json.encodeToString(schedules)
    }

    /**
     * Encodes a list of [BackupAttendanceDto] to a compact JSON string.
     */
    fun serializeAttendance(attendance: List<BackupAttendanceDto>): String {
        return json.encodeToString(attendance)
    }
}
