package com.zscreencontrol.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zscreencontrol.core.Prefs
import com.zscreencontrol.core.ScreenOffController

/**
 * Backstop AlarmManager: dipanggil walau app sedang tidur/dibekukan sistem
 * (termasuk doze di ColorOS). Kalau timer masih berjalan, matikan layar.
 */
class SleepTimerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SleepTimerService.ACTION_FIRE) return
        if (!Prefs.isTimerRunning(context)) return
        Prefs.clearTimer(context)
        context.stopService(Intent(context, SleepTimerService::class.java))
        ScreenOffController.turnOff(context)
    }
}
