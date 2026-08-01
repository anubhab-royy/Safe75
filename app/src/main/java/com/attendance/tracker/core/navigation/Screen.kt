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

    // Subject Detail Screens (Taking over full screen - no bottom bar)
    object AddEditSubject : Screen("add_edit_subject?subjectId={subjectId}") {
        fun createRoute(subjectId: Long = -1L): String {
            return "add_edit_subject?subjectId=$subjectId"
        }
    }

    // Future Module Placeholders (For scalability in Phase 2)
    object Ocr : Screen("ocr_dest")
    object Planner : Screen("planner_dest")
    object Semester : Screen("semester_dest")
    object Backup : Screen("backup_dest")
}
