package com.attendance.tracker.feature.widget

/**
 * Immutable snapshot of the data rendered by the home screen widget.
 *
 * Produced by [WidgetDataProvider] on a background dispatcher and rendered by
 * the Glance composable, so the widget never touches the database directly.
 */
data class WidgetSummary(
    val overallPercentage: Double,
    val todayClasses: Int,
    val todayLogged: Int,
    val nextClassSubject: String?,
    val nextClassStartTime: String?,
    val nextClassEndTime: String?,
    val nextClassRoom: String?,
    val hasData: Boolean
) {
    companion object {
        val EMPTY = WidgetSummary(
            overallPercentage = 0.0,
            todayClasses = 0,
            todayLogged = 0,
            nextClassSubject = null,
            nextClassStartTime = null,
            nextClassEndTime = null,
            nextClassRoom = null,
            hasData = false
        )
    }
}
