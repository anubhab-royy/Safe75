package com.attendance.tracker.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.attendance.tracker.MainActivity
import com.attendance.tracker.R

/**
 * Handles creation of notification channels and helper calls to dispatch notifications.
 */
object TrackerNotificationManager {

    const val CHANNEL_MORNING = "channel_morning"
    const val CHANNEL_POST_CLASS = "channel_post_class"
    const val CHANNEL_MISSED = "channel_missed"

    const val NOTIFICATION_ID_MORNING = 1001
    const val NOTIFICATION_ID_POST_CLASS = 1002
    const val NOTIFICATION_ID_MISSED = 1003

    /**
     * Initializes notification channels inside application startup.
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val morningChannel = NotificationChannel(
                CHANNEL_MORNING,
                "Today's Schedule Reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows classes scheduled for today in the morning."
            }

            val postClassChannel = NotificationChannel(
                CHANNEL_POST_CLASS,
                "Attendance Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you to mark attendance after a class slot ends."
            }

            val missedChannel = NotificationChannel(
                CHANNEL_MISSED,
                "Missed Log Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts you at night if any classes were left unmarked."
            }

            manager.createNotificationChannel(morningChannel)
            manager.createNotificationChannel(postClassChannel)
            manager.createNotificationChannel(missedChannel)
        }
    }

    /**
     * Shows today's classes summary notification.
     */
    fun showMorningReminder(context: Context, classesSummary: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MORNING)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Today's Schedule")
            .setContentText(classesSummary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(classesSummary))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_MORNING, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    /**
     * Shows post-class logging notification with action buttons.
     */
    fun showPostClassReminder(
        context: Context,
        scheduleId: Long,
        subjectId: Long,
        subjectName: String,
        timeSlot: String
    ) {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            1,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Actions intents mapping direct mark Broadcasts
        val presentIntent = Intent(context, AttendanceActionReceiver::class.java).apply {
            action = "ACTION_MARK_PRESENT"
            putExtra("EXTRA_SCHEDULE_ID", scheduleId)
            putExtra("EXTRA_SUBJECT_ID", subjectId)
        }
        val presentPending = PendingIntent.getBroadcast(
            context,
            10,
            presentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val absentIntent = Intent(context, AttendanceActionReceiver::class.java).apply {
            action = "ACTION_MARK_ABSENT"
            putExtra("EXTRA_SCHEDULE_ID", scheduleId)
            putExtra("EXTRA_SUBJECT_ID", subjectId)
        }
        val absentPending = PendingIntent.getBroadcast(
            context,
            11,
            absentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(context, AttendanceActionReceiver::class.java).apply {
            action = "ACTION_MARK_CANCELLED"
            putExtra("EXTRA_SCHEDULE_ID", scheduleId)
            putExtra("EXTRA_SUBJECT_ID", subjectId)
        }
        val cancelPending = PendingIntent.getBroadcast(
            context,
            12,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_POST_CLASS)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle("Class Finished: $subjectName")
            .setContentText("Did you attend the $timeSlot slot?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.checkbox_on_background, "Present", presentPending)
            .addAction(android.R.drawable.checkbox_off_background, "Absent", absentPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                (NOTIFICATION_ID_POST_CLASS + scheduleId).toInt(),
                notification
            )
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    /**
     * Shows end-of-day unmarked class alert.
     */
    fun showMissedReminder(context: Context, unmarkedCount: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MISSED)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Missed Recording Attendance")
            .setContentText("You forgot to record attendance for $unmarkedCount classes today.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_MISSED, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
}
