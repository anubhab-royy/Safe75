package com.attendance.tracker.core.worker

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for [StartupScheduler.nextOccurrence] delay computation.
 *
 * Expected values mirror the pre-existing scheduling semantics exercised by
 * [com.attendance.tracker.core.notification.NotificationSchedulerTest].
 */
class StartupSchedulerTest {

    private val scheduler = StartupScheduler(
        dispatcherProvider = object : com.attendance.tracker.core.common.DispatcherProvider {
            override val main = kotlinx.coroutines.Dispatchers.Unconfined
            override val io = kotlinx.coroutines.Dispatchers.Unconfined
            override val default = kotlinx.coroutines.Dispatchers.Unconfined
        }
    )

    private fun at(hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun testNextOccurrence_targetLaterToday_returnsPositiveDelay() {
        assertEquals(3600000L, scheduler.nextOccurrence(at(7, 0), 8, 0))
    }

    @Test
    fun testNextOccurrence_targetPassed_rollsToTomorrow() {
        assertEquals(82800000L, scheduler.nextOccurrence(at(9, 0), 8, 0))
    }

    @Test
    fun testNextOccurrence_eveningTarget() {
        assertEquals(3600000L, scheduler.nextOccurrence(at(20, 0), 21, 0))
        assertEquals(82800000L, scheduler.nextOccurrence(at(22, 0), 21, 0))
    }

    @Test
    fun testNextOccurrence_exactMatch_returnsZeroDelay() {
        assertEquals(0L, scheduler.nextOccurrence(at(8, 0), 8, 0))
    }
}
