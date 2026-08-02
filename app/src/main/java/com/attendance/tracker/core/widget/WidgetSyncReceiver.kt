package com.attendance.tracker.core.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Refreshes the home screen widget when the date/time/timezone changes or the
 * device reboots.
 *
 * Registered in the manifest for these system broadcasts (all exempt from
 * Android 8+ implicit-broadcast restrictions):
 * - [Intent.ACTION_DATE_CHANGED] (fires at midnight)
 * - [Intent.ACTION_TIME_CHANGED] / [Intent.ACTION_TIMEZONE_CHANGED]
 * - [Intent.ACTION_BOOT_COMPLETED] after reboot
 */
class WidgetSyncReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        WidgetRefreshScheduler.schedule(context)
    }
}
