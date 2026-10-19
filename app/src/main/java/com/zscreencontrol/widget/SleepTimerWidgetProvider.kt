package com.zscreencontrol.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.zscreencontrol.MainActivity
import com.zscreencontrol.core.Prefs
import com.zscreencontrol.service.SleepTimerService

/**
 * Widget "Sleep Timer": sisa waktu + tombol Mulai/Berhenti.
 * Mulai memakai durasi terakhir user (default 30 menit).
 */
class SleepTimerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        WidgetUpdater.updateSleepTimer(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_START -> {
                val started = try {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, SleepTimerService::class.java)
                            .setAction(SleepTimerService.ACTION_START)
                            .putExtra(
                                SleepTimerService.EXTRA_MINUTES,
                                Prefs.lastTimerMinutes(context)
                            )
                    )
                    true
                } catch (_: Exception) {
                    false
                }
                if (!started) openApp(context)
            }

            ACTION_STOP -> {
                val delivered = try {
                    context.startService(
                        Intent(context, SleepTimerService::class.java)
                            .setAction(SleepTimerService.ACTION_STOP)
                    )
                    true
                } catch (_: Exception) {
                    false
                }
                if (!delivered) {
                    // Layanan sudah mati: bersihkan manual.
                    context.stopService(Intent(context, SleepTimerService::class.java))
                    Prefs.clearTimer(context)
                    WidgetUpdater.updateSleepTimer(context)
                }
            }
        }
    }

    private fun openApp(context: Context) {
        val open = Intent(context, MainActivity::class.java)
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(open)
        } catch (_: Exception) {
            // tidak ada yang bisa dilakukan
        }
    }

    companion object {
        const val ACTION_START = "com.zscreencontrol.widget.TIMER_START"
        const val ACTION_STOP = "com.zscreencontrol.widget.TIMER_STOP"
    }
}
