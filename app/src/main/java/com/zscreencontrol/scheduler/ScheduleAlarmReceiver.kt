package com.zscreencontrol.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zscreencontrol.core.ScreenOffController

/** Eksekusi jadwal: matikan layar, lalu pasang alarm untuk eksekusi berikutnya. */
class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SleepScheduler.ACTION_FIRE) return
        ScreenOffController.turnOff(context)
        SleepScheduler.scheduleNext(context)
    }
}
