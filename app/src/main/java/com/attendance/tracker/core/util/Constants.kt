package com.attendance.tracker.core.util

/**
 * System-wide constants for database, notifications, backups, and preferences.
 */
object Constants {
    /**
     * Room database file name.
     */
    const val DATABASE_NAME = "attendance_tracker_db"
    
    // Notifications Configuration
    const val REMINDER_NOTIFICATION_CHANNEL_ID = "attendance_reminders_channel"
    const val REMINDER_NOTIFICATION_ID = 5001

    // Backup Configuration
    const val BACKUP_DIRECTORY_NAME = "AttendanceTrackerBackups"
    const val BACKUP_FILE_PREFIX = "backup_attendance_"
    const val BACKUP_FILE_EXTENSION = ".json"

    // Default Attendance Targets
    const val DEFAULT_REQUIRED_ATTENDANCE_PERCENTAGE = 75.0
    const val DEFAULT_PERSONAL_GOAL_PERCENTAGE = 85.0

    // Preference Datastore Keys
    const val PREF_THEME_MODE = "theme_mode"
    const val PREF_NOTIFICATIONS_ENABLED = "notifications_enabled"
    const val PREF_LAST_BACKUP_TIMESTAMP = "last_backup_timestamp"
}
