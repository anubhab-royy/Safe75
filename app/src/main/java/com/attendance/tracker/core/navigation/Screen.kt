package com.attendance.tracker.core.navigation

/**
 * Sealed class representing application routes and nested graphs for Navigation Compose.
 */
sealed class Screen(val route: String) {
    // Graph Routes
    object RootGraph : Screen("root_graph")
    object MainGraph : Screen("main_graph")

    // Root Screens
    object Splash : Screen("splash")
    object Welcome : Screen("welcome")

    // Main Graph Screens (Bottom Tab Destinations)
    object Dashboard : Screen("dashboard_dest")
    object Subjects : Screen("subjects_dest")
    object Schedule : Screen("schedule_dest")
    object Settings : Screen("settings_dest")

    // Detail Screens (Taking over full screen - no bottom bar)
    object AddEditSubject : Screen("add_edit_subject?subjectId={subjectId}") {
        fun createRoute(subjectId: Long = -1L): String {
            return "add_edit_subject?subjectId=$subjectId"
        }
    }

    object AddEditSchedule : Screen("add_edit_schedule?scheduleId={scheduleId}") {
        fun createRoute(scheduleId: Long = -1L): String {
            return "add_edit_schedule?scheduleId=$scheduleId"
        }
    }

    object BackfillWizard : Screen("backfill_wizard?subjectId={subjectId}") {
        fun createRoute(subjectId: Long = -1L): String {
            return "backfill_wizard?subjectId=$subjectId"
        }
    }

    object AttendanceHistory : Screen("attendance_history")

    object AttendanceDetails : Screen("attendance_details?attendanceId={attendanceId}") {
        fun createRoute(attendanceId: Long): String {
            return "attendance_details?attendanceId=$attendanceId"
        }
    }

    object AttendanceSimulator : Screen("attendance_simulator")

    object LeavePlanner : Screen("leave_planner")

    object NotificationSettings : Screen("notification_settings")

    // Data Management & Semester Lifecycle (Phase 7)
    object Restore : Screen("restore_dest")
    object SemesterReset : Screen("semester_reset_dest")
    object Archive : Screen("archive_dest")
    object Integrity : Screen("integrity_dest")
    object BugReport : Screen("bug_report_dest")

    object ArchiveDetails : Screen("archive_details?archiveId={archiveId}") {
        fun createRoute(archiveId: Long): String {
            return "archive_details?archiveId=$archiveId"
        }
    }

    // Future Module Placeholders (For scalability in Phase 2)
    object Ocr : Screen("ocr_dest")
    object Planner : Screen("planner_dest")
    object Semester : Screen("semester_dest")
    object SemesterSetup : Screen("semester_setup?semesterId={semesterId}") {
        fun createRoute(semesterId: Long = -1L): String {
            return "semester_setup?semesterId=$semesterId"
        }
    }
    object Backup : Screen("backup_dest")
}
