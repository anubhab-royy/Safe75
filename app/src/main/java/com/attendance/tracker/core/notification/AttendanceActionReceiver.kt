package com.attendance.tracker.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.attendance.tracker.core.model.AttendanceStatus
import com.attendance.tracker.domain.model.Attendance
import com.attendance.tracker.domain.usecase.attendance.MarkAttendanceUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * BroadcastReceiver executing direct database markings on notification click triggers.
 */
@AndroidEntryPoint
class AttendanceActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var markAttendanceUseCase: MarkAttendanceUseCase

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra("EXTRA_SCHEDULE_ID", -1L)
        val subjectId = intent.getLongExtra("EXTRA_SUBJECT_ID", -1L)
        if (scheduleId == -1L || subjectId == -1L) return

        val status = when (intent.action) {
            "ACTION_MARK_PRESENT" -> AttendanceStatus.PRESENT
            "ACTION_MARK_ABSENT" -> AttendanceStatus.ABSENT
            "ACTION_MARK_CANCELLED" -> AttendanceStatus.CANCELLED
            else -> return
        }

        val notificationId = (TrackerNotificationManager.NOTIFICATION_ID_POST_CLASS + scheduleId).toInt()
        NotificationManagerCompat.from(context).cancel(notificationId)

        CoroutineScope(Dispatchers.IO).launch {
            val log = Attendance(
                subjectId = subjectId,
                scheduleId = scheduleId,
                date = LocalDate.now(),
                status = status,
                remarks = "Quick logged from notification"
            )
            markAttendanceUseCase(log)
        }
    }
}
