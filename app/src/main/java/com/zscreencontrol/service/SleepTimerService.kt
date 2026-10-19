package com.zscreencontrol.service

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.zscreencontrol.MainActivity
import com.zscreencontrol.R
import com.zscreencontrol.ZScreenApp
import com.zscreencontrol.core.Prefs
import com.zscreencontrol.core.ScreenOffController
import com.zscreencontrol.widget.WidgetUpdater
import java.util.Locale

/**
 * Foreground service penghitung mundur sleep timer.
 *
 * Sumber kebenaran waktu = Prefs (waktu akhir dalam elapsedRealtime), sehingga
 * UI dan service selalu sinkron walau proses sempat mati. Saat waktu habis,
 * layar dimatikan lewat [ScreenOffController] (fallback bertingkat).
 * AlarmManager dipasang sebagai backstop agar tetap jalan saat HP doze.
 */
class SleepTimerService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var tickCount = 0

    private val ticker = object : Runnable {
        override fun run() {
            if (!Prefs.isTimerRunning(this@SleepTimerService)) return
            if (remainingMs() <= 0L) {
                finishTimer()
            } else {
                tickCount++
                if (tickCount % 15 == 0) {
                    // Segarkan widget tiap ±15 detik utk teks sisa waktu.
                    WidgetUpdater.updateSleepTimer(this@SleepTimerService)
                }
                handler.postDelayed(this, TICK_MS)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START ->
                startTimer(intent.getIntExtra(EXTRA_MINUTES, DEFAULT_MINUTES).toLong() * 60_000L)
            ACTION_EXTEND -> extendTimer(EXTEND_MS)
            ACTION_STOP -> stopTimer()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }

    private fun remainingMs(): Long =
        (Prefs.timerEndElapsed(this) - SystemClock.elapsedRealtime()).coerceAtLeast(0L)

    private fun startTimer(durationMs: Long) {
        if (durationMs <= 0L) return
        Prefs.setLastTimerMinutes(this, (durationMs / 60_000L).toInt().coerceAtLeast(1))
        val end = SystemClock.elapsedRealtime() + durationMs
        Prefs.setTimer(this, durationMs, end)
        startForeground(NOTIFICATION_ID, buildNotification(end))
        scheduleAlarm(end)
        handler.removeCallbacks(ticker)
        handler.postDelayed(ticker, TICK_MS)
        WidgetUpdater.updateSleepTimer(this)
    }

    private fun extendTimer(extraMs: Long) {
        if (!Prefs.isTimerRunning(this)) return
        val newEnd = Prefs.timerEndElapsed(this) + extraMs
        Prefs.setTimer(this, Prefs.timerDurationMs(this) + extraMs, newEnd)
        getSystemService(android.app.NotificationManager::class.java)
            ?.notify(NOTIFICATION_ID, buildNotification(newEnd))
        scheduleAlarm(newEnd)
        WidgetUpdater.updateSleepTimer(this)
    }

    private fun stopTimer() {
        Prefs.clearTimer(this)
        cancelAlarm()
        handler.removeCallbacks(ticker)
        WidgetUpdater.updateSleepTimer(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun finishTimer() {
        Prefs.clearTimer(this)
        WidgetUpdater.updateSleepTimer(this)
        ScreenOffController.turnOff(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ---------- Alarm backstop ----------

    private fun scheduleAlarm(endElapsedRealtime: Long) {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = alarmPendingIntent()
        try {
            if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, endElapsedRealtime, pi)
            } else {
                am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, endElapsedRealtime, pi)
            }
        } catch (_: SecurityException) {
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, endElapsedRealtime, pi)
        }
    }

    private fun cancelAlarm() {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(alarmPendingIntent())
    }

    private fun alarmPendingIntent(): PendingIntent = alarmPendingIntent(this)

    // ---------- Notifikasi ----------

    private fun buildNotification(endElapsedRealtime: Long): Notification {
        val remainingMs = (endElapsedRealtime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        val stopIntent = PendingIntent.getService(
            this,
            REQUEST_STOP,
            Intent(this, SleepTimerService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = PendingIntent.getActivity(
            this,
            REQUEST_OPEN,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, ZScreenApp.CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_tile_screen_off)
            .setContentTitle(getString(R.string.notif_timer_title))
            .setContentText(
                getString(R.string.notif_timer_text) + " (" + formatDuration(remainingMs) + ")"
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.notif_timer_stop), stopIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        const val ACTION_START = "com.zscreencontrol.action.TIMER_START"
        const val ACTION_STOP = "com.zscreencontrol.action.TIMER_STOP"
        const val ACTION_EXTEND = "com.zscreencontrol.action.TIMER_EXTEND"
        const val ACTION_FIRE = "com.zscreencontrol.action.TIMER_FIRE"

        const val EXTRA_MINUTES = "extra_minutes"
        const val DEFAULT_MINUTES = 30

        private const val EXTEND_MS = 10 * 60_000L
        private const val TICK_MS = 1_000L
        private const val NOTIFICATION_ID = 10
        private const val REQUEST_ALARM = 11
        private const val REQUEST_STOP = 12
        private const val REQUEST_OPEN = 13

        /** PendingIntent alarm backstop (dibagikan ke luar utk widget/tile). */
        fun alarmPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, SleepTimerAlarmReceiver::class.java).setAction(ACTION_FIRE)
            return PendingIntent.getBroadcast(
                context,
                REQUEST_ALARM,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /** Format mm:ss, atau h:mm:ss kalau lebih dari 1 jam. */
        fun formatDuration(ms: Long): String {
            val totalSeconds = ms / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }
        }
    }
}
