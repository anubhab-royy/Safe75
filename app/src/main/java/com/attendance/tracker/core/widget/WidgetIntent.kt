package com.attendance.tracker.core.widget

import com.attendance.tracker.core.navigation.Screen

/**
 * Centralizes the intent contracts used by the home screen widget quick actions.
 *
 * The widget can open the app directly at a specific destination (bypassing the
 * splash/onboarding flow) via an [EXTRA_DESTINATION] string extra.
 */
object WidgetIntent {

    /** Intent extra holding the requested start destination route. */
    const val EXTRA_DESTINATION = "widget_start_destination"

    /** Widget "Open Dashboard" action. */
    const val DESTINATION_DASHBOARD = "dashboard"
    /** Widget "Open Attendance" action. */
    const val DESTINATION_ATTENDANCE_HISTORY = "attendance_history"
    /** Widget "Open Schedule" action. */
    const val DESTINATION_SCHEDULE = "schedule"
    /** Widget "Open Settings" action. */
    const val DESTINATION_SETTINGS = "settings"

    /**
     * Maps a widget destination token to a Navigation Compose route.
     *
     * Returns the main-graph route when the token is unknown so the app always
     * lands on a valid screen.
     */
    fun routeFor(destination: String?): String = when (destination) {
        DESTINATION_ATTENDANCE_HISTORY -> Screen.AttendanceHistory.route
        DESTINATION_SCHEDULE -> Screen.Schedule.route
        DESTINATION_SETTINGS -> Screen.Settings.route
        DESTINATION_DASHBOARD -> Screen.Dashboard.route
        else -> Screen.Dashboard.route
    }

    /** True when [destination] should open the main graph directly (skip splash). */
    fun opensMainGraph(destination: String?): Boolean = when (destination) {
        DESTINATION_DASHBOARD, DESTINATION_SCHEDULE, DESTINATION_SETTINGS -> true
        else -> false
    }
}
