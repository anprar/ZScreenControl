package com.zscreencontrol.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Setelah reboot, alarm AlarmManager hilang — pasang ulang di sini.
 */
class ScheduleBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            SleepScheduler.scheduleNext(context)
        }
    }
}
