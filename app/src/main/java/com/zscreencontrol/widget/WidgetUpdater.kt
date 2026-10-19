package com.zscreencontrol.widget

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import com.zscreencontrol.R
import com.zscreencontrol.core.Prefs
import com.zscreencontrol.service.SleepTimerService

/**
 * Pembaruan widget (RemoteViews klasik — tanpa dependensi tambahan).
 * Dipanggil dari mereka yang mengubah status timer (SleepTimerService) dan
 * masing-masing provider saat sistem meng-update widget.
 */
object WidgetUpdater {

    fun updateAll(context: Context) {
        updateScreenOff(context)
        updateSleepTimer(context)
    }

    fun updateScreenOff(context: Context) {
        val manager = android.appwidget.AppWidgetManager.getInstance(context) ?: return
        val ids = manager.getAppWidgetIds(
            ComponentName(context, ScreenOffWidgetProvider::class.java)
        ) ?: return
        if (ids.isEmpty()) return

        val views = RemoteViews(context.packageName, R.layout.widget_screen_off)
        views.setOnClickPendingIntent(
            R.id.widget_screen_off_root,
            screenOffPendingIntent(context)
        )
        manager.updateAppWidget(ids, views)
    }

    fun updateSleepTimer(context: Context) {
        val manager = android.appwidget.AppWidgetManager.getInstance(context) ?: return
        val ids = manager.getAppWidgetIds(
            ComponentName(context, SleepTimerWidgetProvider::class.java)
        ) ?: return
        if (ids.isEmpty()) return

        val running = Prefs.isTimerRunning(context)
        val remaining = if (running) {
            (Prefs.timerEndElapsed(context) - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        } else 0L

        val views = RemoteViews(context.packageName, R.layout.widget_sleep_timer)
        views.setTextViewText(
            R.id.widget_timer_text,
            if (running) SleepTimerService.formatDuration(remaining)
            else context.getString(R.string.widget_timer_idle)
        )
        views.setOnClickPendingIntent(
            R.id.widget_timer_start,
            providerPendingIntent(context, SleepTimerWidgetProvider.ACTION_START, 51)
        )
        views.setOnClickPendingIntent(
            R.id.widget_timer_stop,
            providerPendingIntent(context, SleepTimerWidgetProvider.ACTION_STOP, 52)
        )
        manager.updateAppWidget(ids, views)
    }

    private fun providerPendingIntent(
        context: Context,
        action: String,
        requestCode: Int
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, SleepTimerWidgetProvider::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun screenOffPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            53,
            Intent(context, ScreenOffWidgetProvider::class.java)
                .setAction(ScreenOffWidgetProvider.ACTION_SCREEN_OFF),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
