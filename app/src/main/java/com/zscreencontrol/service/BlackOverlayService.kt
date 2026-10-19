package com.zscreencontrol.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.zscreencontrol.R
import com.zscreencontrol.ZScreenApp

/**
 * Overlay hitam penuh — "screen off palsu": layar fisik tetap menyala,
 * hanya visualnya yang ditutup. Dipakai sebagai fallback terakhir
 * ketika Device Admin / Accessibility tidak tersedia.
 *
 * Ketuk di mana saja untuk keluar. Butuh izin SYSTEM_ALERT_WINDOW.
 */
class BlackOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        showOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISMISS) stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
                // view sudah lepas
            }
        }
        overlayView = null
        super.onDestroy()
    }

    private fun showOverlay() {
        if (overlayView != null) return
        val view = View(this).apply {
            setBackgroundColor(Color.BLACK)
            isClickable = true
            setOnClickListener { stopSelf() }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        try {
            windowManager.addView(view, params)
            overlayView = view
        } catch (_: Exception) {
            // izin overlay dicabut di tengah jalan
            stopSelf()
        }
    }

    private fun buildNotification(): Notification {
        val dismissIntent = PendingIntent.getService(
            this,
            REQUEST_DISMISS,
            Intent(this, BlackOverlayService::class.java).setAction(ACTION_DISMISS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, ZScreenApp.CHANNEL_OVERLAY)
            .setSmallIcon(R.drawable.ic_tile_screen_off)
            .setContentTitle(getString(R.string.notif_overlay_title))
            .setContentText(getString(R.string.notif_overlay_text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(dismissIntent)
            .addAction(0, getString(R.string.notif_overlay_stop), dismissIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        const val ACTION_DISMISS = "com.zscreencontrol.action.OVERLAY_DISMISS"
        private const val NOTIFICATION_ID = 20
        private const val REQUEST_DISMISS = 21
    }
}
