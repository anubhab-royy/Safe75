package com.attendance.tracker.data.local.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validates the registered Room migrations against the exported schemas
 * (app/schemas). Confirms existing rows survive an upgrade and that the
 * migrated schema matches the target version exactly (Room 2.8 validates
 * columns, nullability and default values).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate2To3_addsStartEndDateAndPreservesData() {
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL(
                "INSERT INTO `semester_versions` (`name`, `isActive`, `createdAt`) " +
                    "VALUES ('Semester 1', 1, 1000)"
            )
            execSQL(
                "INSERT INTO `subjects` " +
                    "(`name`, `color`, `requiredAttendancePercentage`, `personalAttendanceGoal`, `createdAt`, `updatedAt`) " +
                    "VALUES ('Mathematics', 16711680, 75, 85, 1000, 1000)"
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 3, true, *Migrations.ALL)

        migrated.query(
            "SELECT `name`, `isActive`, `createdAt`, `startDate`, `endDate` FROM `semester_versions`"
        ).use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("Semester 1", cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
            assertEquals(1000L, cursor.getLong(2))
            // startDate/endDate are backfilled by the migration with the current
            // date (ISO-8601), so they must be present and parseable.
            assertNotNull(cursor.getString(3))
            assertNotNull(cursor.getString(4))
            assertEquals(true, cursor.getString(3).isNotBlank())
            assertEquals(true, cursor.getString(4).isNotBlank())
        }
    }

    @Test
    fun migrate3To4_addsBugReportQueue() {
        helper.createDatabase(TEST_DB_V4, 3).close()

        val migrated = helper.runMigrationsAndValidate(TEST_DB_V4, 4, true, *Migrations.ALL)

        migrated.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'bug_report_queue'")
            .use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals("bug_report_queue", cursor.getString(0))
            }
    }

    companion object {
        private const val TEST_DB = "migration-test.db"
        private const val TEST_DB_V4 = "migration-test-v4.db"
    }
}
