package com.zscreencontrol.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.zscreencontrol.MainActivity
import com.zscreencontrol.core.OffMethod
import com.zscreencontrol.core.ScreenOffController

/**
 * Widget "Matikan Layar": satu tombol, ketuk -> turnOff (fallback bertingkat).
 * Jika tidak ada metode yang tersedia, buka aplikasi (agar user mengaktifkan izin).
 */
class ScreenOffWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        WidgetUpdater.updateScreenOff(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_SCREEN_OFF) return

        val method = ScreenOffController.turnOff(context)
        if (method == OffMethod.NONE) {
            // Tidak ada metode: buka aplikasi agar user mengaktifkan izin dulu.
            val open = Intent(context, MainActivity::class.java)
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(open)
            } catch (_: Exception) {
                // tidak ada yang bisa dilakukan
            }
        }
    }

    companion object {
        const val ACTION_SCREEN_OFF = "com.zscreencontrol.widget.SCREEN_OFF"
    }
}
