package com.attendance.tracker.core.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Round-trip and field-preservation tests for [BackupSerializer] and [BackupDeserializer].
 */
class BackupSerializerTest {

    private val serializer = BackupSerializer()
    private val deserializer = BackupDeserializer()

    private fun sampleData(): BackupData {
        val now = 1_700_000_000_000L
        return BackupData(
            metadata = BackupMetadata(
                backupVersion = 1,
                appVersion = "1.0",
                schemaVersion = 1,
                createdAt = now
            ),
            subjects = listOf(
                BackupSubjectDto(
                    id = 1L, name = "Mathematics", facultyName = "Dr. Smith",
                    color = 0xFF0000, requiredAttendancePercentage = 75,
                    personalAttendanceGoal = 85, createdAt = now, updatedAt = now
                )
            ),
            schedules = listOf(
                BackupScheduleDto(
                    id = 10L, subjectId = 1L, dayOfWeek = "Monday",
                    startTime = "09:00", endTime = "10:00", room = "201",
                    teacherOverride = null, versionId = 5L, createdAt = now, updatedAt = now
                )
            ),
            semesterVersions = listOf(
                BackupSemesterDto(id = 5L, name = "Fall 2026", isActive = true, createdAt = now)
            ),
            attendanceRecords = listOf(
                BackupAttendanceDto(
                    id = 100L, subjectId = 1L, scheduleId = 10L, date = "2026-08-02",
                    status = "PRESENT", remarks = null, createdAt = now, updatedAt = now
                )
            ),
            settings = BackupSettingsDto(
                themeMode = "DARK", notificationsEnabled = true,
                morningReminderEnabled = true, attendanceReminderEnabled = true,
                missedReminderEnabled = false
            )
        )
    }

    @Test
    fun testRoundTrip_preservesAllFields() {
        val json = serializer.serialize(sampleData())
        val result = deserializer.deserialize(json)

        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()

        assertEquals(1, parsed.subjects.size)
        assertEquals("Mathematics", parsed.subjects.first().name)
        assertEquals("Dr. Smith", parsed.subjects.first().facultyName)

        assertEquals(1, parsed.schedules.size)
        assertEquals("Monday", parsed.schedules.first().dayOfWeek)
        assertEquals("09:00", parsed.schedules.first().startTime)

        assertEquals(1, parsed.semesterVersions.size)
        assertEquals("Fall 2026", parsed.semesterVersions.first().name)

        assertEquals(1, parsed.attendanceRecords.size)
        assertEquals("PRESENT", parsed.attendanceRecords.first().status)

        assertEquals("DARK", parsed.settings.themeMode)
        assertEquals(1_700_000_000_000L, parsed.metadata.createdAt)
    }

    @Test
    fun testSerialize_producesValidJsonDocument() {
        val json = serializer.serialize(sampleData())
        assertTrue(json.contains("\"subjects\""))
        assertTrue(json.contains("\"metadata\""))
        assertTrue(json.contains("\"attendanceRecords\""))
        assertTrue(json.contains("\"semesterVersions\""))
    }

    @Test
    fun testDeserialize_blankString_returnsFailure() {
        val result = deserializer.deserialize("   ")
        assertTrue(result.isFailure)
    }

    @Test
    fun testDeserialize_corruptJson_returnsFailure() {
        val result = deserializer.deserialize("{ not valid json !!!")
        assertTrue(result.isFailure)
    }

    @Test
    fun testSerializeSubjects_embeddedListRoundTrip() {
        val subjects = listOf(
            BackupSubjectDto(1L, "Physics", createdAt = 100L, updatedAt = 100L),
            BackupSubjectDto(2L, "Chemistry", createdAt = 100L, updatedAt = 100L)
        )
        val json = serializer.serializeSubjects(subjects)
        val parsed = deserializer.deserializeSubjects(json)
        assertEquals(2, parsed.size)
        assertEquals("Chemistry", parsed[1].name)
    }

    @Test
    fun testDeserializeSubjects_corruptJson_returnsEmptyList() {
        assertTrue(deserializer.deserializeSubjects("@@@").isEmpty())
    }
}
