package com.attendance.tracker.core.notification

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class NotificationSchedulerTest {

    private fun calculateDelay(targetHour: Int, targetMinute: Int, nowCal: Calendar): Long {
        val dueCal = Calendar.getInstance().apply {
            timeInMillis = nowCal.timeInMillis
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
        }
        if (dueCal.before(nowCal)) {
            dueCal.add(Calendar.HOUR_OF_DAY, 24)
        }
        return dueCal.timeInMillis - nowCal.timeInMillis
    }

    @Test
    fun testSchedulerDelay_targetInFuture_calculatesCorrectPositiveDelay() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        // Target: 8:00 AM today (1 hour in future)
        val delay = calculateDelay(8, 0, now)
        assertEquals(3600000L, delay) // 1 hour in milliseconds
    }

    @Test
    fun testSchedulerDelay_targetInPast_calculatesNextDayDelay() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        // Target: 8:00 AM (already passed, should set for tomorrow)
        // Delay should be 23 hours
        val delay = calculateDelay(8, 0, now)
        assertEquals(82800000L, delay) // 23 hours in milliseconds
    }
}
