package com.attendance.tracker.core.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [BackupFileProvider] file-name conventions.
 */
class BackupFileProviderTest {

    private val provider = BackupFileProvider()

    @Test
    fun testCreateDefaultFileName_isTimestampedAndJson() {
        val name = provider.createDefaultFileName(1_700_000_000_000L)
        assertTrue(name.startsWith("attendance_tracker_backup_"))
        assertTrue(name.endsWith(".json"))
        assertFalse(name.contains(" "))
    }

    @Test
    fun testCreateFileName_customPrefix() {
        val name = provider.createFileName("my_custom_", 1_700_000_000_000L)
        assertTrue(name.startsWith("my_custom_"))
        assertTrue(name.endsWith(".json"))
    }

    @Test
    fun testHasJsonExtension_caseInsensitive() {
        assertTrue(provider.hasJsonExtension("data.json"))
        assertTrue(provider.hasJsonExtension("DATA.JSON"))
        assertTrue(provider.hasJsonExtension("backup.some.json"))
        assertFalse(provider.hasJsonExtension("data.txt"))
        assertFalse(provider.hasJsonExtension("json"))
        assertFalse(provider.hasJsonExtension(""))
    }

    @Test
    fun testSuggestedExportFileName_matchesConvention() {
        val name = provider.suggestedExportFileName(1_700_000_000_000L)
        assertEquals(provider.createDefaultFileName(1_700_000_000_000L), name)
    }
}
