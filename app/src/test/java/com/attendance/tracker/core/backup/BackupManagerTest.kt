package com.attendance.tracker.core.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Unit tests for [BackupManager] parsing pipeline and error classification.
 */
class BackupManagerTest {

    private val manager = BackupManager(
        serializer = BackupSerializer(),
        deserializer = BackupDeserializer(),
        validator = BackupValidator(),
        fileProvider = BackupFileProvider()
    )

    private val now = 1_700_000_000_000L

    private fun validJson(): String {
        val data = BackupData(
            metadata = BackupMetadata(backupVersion = 1, appVersion = "1.0", schemaVersion = 1, createdAt = now),
            subjects = listOf(BackupSubjectDto(1L, "Math", createdAt = now, updatedAt = now)),
            schedules = emptyList(),
            semesterVersions = emptyList(),
            attendanceRecords = emptyList()
        )
        return BackupSerializer().serialize(data)
    }

    @Test
    fun testParseBackup_validJson_returnsParsed() {
        val result = manager.parseBackup(validJson())
        assertTrue(result is BackupParseResult.Parsed)
        assertEquals("Math", (result as BackupParseResult.Parsed).data.subjects.first().name)
    }

    @Test
    fun testParseBackup_blank_returnsInvalidJson() {
        val result = manager.parseBackup("   ")
        assertTrue(result is BackupParseResult.Rejected)
        assertTrue((result as BackupParseResult.Rejected).error is BackupError.InvalidJson)
    }

    @Test
    fun testParseBackup_corruptJson_returnsInvalidJson() {
        val result = manager.parseBackup("{ broken json")
        assertTrue(result is BackupParseResult.Rejected)
        assertTrue((result as BackupParseResult.Rejected).error is BackupError.InvalidJson)
    }

    @Test
    fun testParseBackup_unsupportedSchema_returnsUnsupportedSchema() {
        val json = BackupSerializer().serialize(
            BackupData(
                metadata = BackupMetadata(backupVersion = 1, appVersion = "1.0", schemaVersion = 99, createdAt = now)
            )
        )
        val result = manager.parseBackup(json)
        assertTrue(result is BackupParseResult.Rejected)
        assertTrue((result as BackupParseResult.Rejected).error is BackupError.UnsupportedSchema)
    }

    @Test
    fun testParseBackup_newerBackupVersion_returnsOldBackupVersion() {
        val json = BackupSerializer().serialize(
            BackupData(
                metadata = BackupMetadata(backupVersion = 99, appVersion = "2.0", schemaVersion = 1, createdAt = now)
            )
        )
        val result = manager.parseBackup(json)
        assertTrue(result is BackupParseResult.Rejected)
        assertTrue((result as BackupParseResult.Rejected).error is BackupError.OldBackupVersion)
    }

    @Test
    fun testParseBackup_referentialError_returnsInvalidJson() {
        val data = BackupData(
            metadata = BackupMetadata(backupVersion = 1, appVersion = "1.0", schemaVersion = 1, createdAt = now),
            schedules = listOf(
                BackupScheduleDto(10L, 404L, "Monday", "09:00", "10:00", versionId = 5L, createdAt = now, updatedAt = now)
            )
        )
        val result = manager.parseBackup(BackupSerializer().serialize(data))
        assertTrue(result is BackupParseResult.Rejected)
        assertTrue((result as BackupParseResult.Rejected).error is BackupError.InvalidJson)
    }

    @Test
    fun testClassifyError_mapsSecurityToFilePermission() {
        val error = manager.classifyError(SecurityException("denied"))
        assertTrue(error is BackupError.FilePermission)
    }

    @Test
    fun testClassifyError_mapsIOExceptionToStorageUnavailable() {
        val error = manager.classifyError(IOException("device full"))
        assertTrue(error is BackupError.StorageUnavailable)
    }

    @Test
    fun testClassifyError_mapsPassThroughBackupError() {
        val original = BackupError.CorruptedArchive("damaged")
        assertEquals(original, manager.classifyError(original))
    }

    @Test
    fun testClassifyError_mapsUnknownToUnknown() {
        val error = manager.classifyError(IllegalStateException("boom"))
        assertTrue(error is BackupError.Unknown)
    }

    @Test
    fun testCreateFileName_hasTimestampAndExtension() {
        val name = manager.createFileName(1_700_000_000_000L)
        assertTrue(name.endsWith(".json"))
        assertTrue(name.startsWith("attendance_tracker_backup_"))
    }

    @Test
    fun testHasJsonExtension() {
        assertTrue(manager.hasJsonExtension("backup.json"))
        assertTrue(manager.hasJsonExtension("BACKUP.JSON"))
        assertFalse(manager.hasJsonExtension("backup.txt"))
        assertFalse(manager.hasJsonExtension(""))
    }
}
