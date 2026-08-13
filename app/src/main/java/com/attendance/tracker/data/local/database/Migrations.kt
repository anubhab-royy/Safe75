package com.attendance.tracker.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Versioned Room migrations for [AppDatabase].
 *
 * Migration policy (production-safe):
 * - Every schema change MUST add an entry here and bump [AppDatabase] version.
 * - Migrations are registered in ALL build types via `addMigrations(*Migrations.ALL)`
 *   (see `di/DatabaseModule`), so upgrades preserve user data in Release too.
 * - Destructive migration fallback is enabled ONLY in debug builds; Release never
 *   wipes the database, it fails the upgrade instead (crash) so data is never lost.
 *
 * Schema history:
 * - v1 (pre-release only): initial schema. NEVER shipped to production; the
 *   schema changed repeatedly across pre-release commits with no version bump,
 *   so no reliable 1→2 migration can be reconstructed. Development v1 databases
 *   are covered by the debug-only destructive fallback; there is no production
 *   v1 user base.
 * - v2: adds the `archives` table for semester archiving.
 * - v3: adds NOT NULL `startDate` / `endDate` to `semester_versions`.
 */
object Migrations {

    /**
     * v2 → v3: add NOT NULL `startDate` / `endDate` to `semester_versions`.
     *
     * SQLite cannot add a NOT NULL column without a DEFAULT to a populated table,
     * and Room 2.8 validates column default values strictly, so a plain
     * `ADD COLUMN ... NOT NULL DEFAULT` would leave the migrated schema with a
     * default the entity does not declare and fail validation. The table is
     * therefore rebuilt: the columns are first added with a placeholder default
     * (backfilling existing rows with the current date), then the table is
     * recreated with the exact v3 DDL (which has no SQL default), the data is
     * copied across, and the original table is replaced.
     */
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Android's bundled SQLite rejects non-constant ADD COLUMN defaults,
            // so a constant placeholder is used and existing rows are then
            // backfilled with the current date via an UPDATE.
            db.execSQL(
                "ALTER TABLE `semester_versions` ADD COLUMN `startDate` TEXT NOT NULL DEFAULT '1970-01-01'"
            )
            db.execSQL(
                "ALTER TABLE `semester_versions` ADD COLUMN `endDate` TEXT NOT NULL DEFAULT '1970-01-01'"
            )
            db.execSQL(
                "UPDATE `semester_versions` SET `startDate` = date('now'), `endDate` = date('now', '+4 months')"
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `semester_versions_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `isActive` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `startDate` TEXT NOT NULL,
                    `endDate` TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `semester_versions_new` (`id`, `name`, `isActive`, `createdAt`, `startDate`, `endDate`)
                SELECT `id`, `name`, `isActive`, `createdAt`, `startDate`, `endDate` FROM `semester_versions`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `semester_versions`")
            db.execSQL("ALTER TABLE `semester_versions_new` RENAME TO `semester_versions`")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `bug_report_queue` (
                    `reportId` TEXT NOT NULL,
                    `fingerprint` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `appVersion` TEXT NOT NULL,
                    `versionCode` INTEGER NOT NULL,
                    `buildType` TEXT NOT NULL,
                    `androidVersion` TEXT NOT NULL,
                    `sdkVersion` INTEGER NOT NULL,
                    `deviceManufacturer` TEXT NOT NULL,
                    `deviceModel` TEXT NOT NULL,
                    `cpuAbi` TEXT NOT NULL,
                    `locale` TEXT NOT NULL,
                    `crashReportId` TEXT,
                    `userDescription` TEXT NOT NULL,
                    `diagnosticsMetadata` TEXT NOT NULL,
                    `screenshotPath` TEXT,
                    `screenshotContentType` TEXT,
                    `remoteReportId` TEXT,
                    `status` TEXT NOT NULL,
                    `retryCount` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `lastError` TEXT,
                    PRIMARY KEY(`reportId`)
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_bug_report_queue_fingerprint` " +
                    "ON `bug_report_queue` (`fingerprint`)"
            )
        }
    }

    /**
     * Ordered migration list. Register in [androidx.room.RoomDatabase.Builder.addMigrations].
     */
    val ALL: Array<Migration> = arrayOf(
        MIGRATION_2_3,
        MIGRATION_3_4
    )
}
