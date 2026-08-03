package com.attendance.tracker.feature.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.attendance.tracker.MainActivity
import com.attendance.tracker.core.widget.WidgetIntent
import com.attendance.tracker.core.widget.WidgetRefreshScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.EntryPointAccessors

/**
 * Material 3 home screen widget for the Safe75.
 *
 * Shows today's attendance, the overall attendance percentage, today's class
 * count and the next upcoming class, with quick actions to open the dashboard,
 * open attendance history, and refresh the data.
 *
 * Data is loaded on a background dispatcher by [WidgetDataProvider] so the
 * widget render never touches the database on the main thread.
 */
class AttendanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val summary = loadSummary(context)
        provideContent {
            WidgetContent(summary = summary)
        }
    }

    private suspend fun loadSummary(context: Context): WidgetSummary {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        return entryPoint.widgetDataProvider().load()
    }
}

/**
 * Hilt entry point used by the widget to obtain [WidgetDataProvider] outside
 * the normal ViewModel/Activity injection graph.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun widgetDataProvider(): WidgetDataProvider
}

/**
 * Widget receiver registered in the manifest.
 */
class AttendanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AttendanceWidget()
}

/**
 * Refresh action triggered by the widget's refresh button. Enqueues a
 * WorkManager refresh so the widget reflects the latest database state.
 */
class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        WidgetRefreshScheduler.schedule(context)
    }
}

/**
 * Renders the widget content, applying dynamic colors on Android 12+ and a
 * fixed Material 3 palette elsewhere.
 */
@Composable
private fun WidgetContent(summary: WidgetSummary) {
    val context = LocalContext.current
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isSystemDark(context)) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (isSystemDark(context)) darkColorScheme() else lightColorScheme()
    }

    GlanceTheme(colors = ColorProviders(colorScheme)) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(colorScheme.background))
                .padding(12.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attendance",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(colorScheme.onBackground)
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "${summary.overallPercentage.toInt()}% overall",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(colorScheme.primary)
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            Text(
                text = "Today: ${summary.todayLogged}/${summary.todayClasses} classes",
                style = TextStyle(color = ColorProvider(colorScheme.onSurfaceVariant))
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            val nextLabel = summary.nextClassSubject
                ?.let { subject ->
                    val time = summary.nextClassStartTime ?: ""
                    val room = summary.nextClassRoom?.let { " · $it" } ?: ""
                    "Next: $subject at $time$room"
                } ?: "No upcoming class today"
            Text(
                text = nextLabel,
                style = TextStyle(color = ColorProvider(colorScheme.onBackground))
            )

            Spacer(modifier = GlanceModifier.defaultWeight())

            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    text = "Dashboard",
                    onClick = openMainActivity(context, WidgetIntent.DESTINATION_DASHBOARD),
                    modifier = GlanceModifier.defaultWeight()
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Button(
                    text = "Attendance",
                    onClick = openMainActivity(context, WidgetIntent.DESTINATION_ATTENDANCE_HISTORY),
                    modifier = GlanceModifier.defaultWeight()
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
                Button(
                    text = "Refresh",
                    onClick = actionRunCallback<RefreshWidgetAction>(),
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }
}

private fun openMainActivity(context: Context, destination: String) = actionStartActivity(
    Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_MAIN
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(WidgetIntent.EXTRA_DESTINATION, destination)
    }
)

private fun isSystemDark(context: Context): Boolean =
    (context.resources.configuration.uiMode and
        android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES
