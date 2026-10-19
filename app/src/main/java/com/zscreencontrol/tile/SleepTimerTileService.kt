package com.zscreencontrol.tile

import android.content.Intent
import android.os.SystemClock
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.zscreencontrol.MainActivity
import com.zscreencontrol.R
import com.zscreencontrol.core.Prefs
import com.zscreencontrol.service.SleepTimerService

/**
 * Quick Settings Tile "Sleep Timer" (jembatan ketiga ke sleep timer).
 * Ketuk: mulai (durasi terakhir, default 30 menit) / berhenti.
 * Arahkan ke subjudul: sisa waktu saat timer aktif.
 */
class SleepTimerTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val running = Prefs.isTimerRunning(this)
        if (running) {
            // menghentikan layanan yang sedang berjalan: cukup startService biasa.
            // (startForegroundService utk aksi STOP berbahaya: layanan selalu
            // diwajibkan memanggil startForeground(), dan stopTimer memang tidak.)
            val delivered = try {
                startService(
                    Intent(this, SleepTimerService::class.java)
                        .setAction(SleepTimerService.ACTION_STOP)
                )
                true
            } catch (_: Exception) {
                false
            }
            if (!delivered) {
                // Layanan sudah mati (app dibekukan sistem): bersihkan manual
                // supaya alarm cadangan tidak memicu pematikan layar.
                stopService(Intent(this, SleepTimerService::class.java))
                Prefs.clearTimer(this)
            }
        } else {
            val started = try {
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, SleepTimerService::class.java)
                        .setAction(SleepTimerService.ACTION_START)
                        .putExtra(SleepTimerService.EXTRA_MINUTES, Prefs.lastTimerMinutes(this))
                )
                true
            } catch (_: Exception) {
                false
            }
            if (!started) {
                // Fallback: buka aplikasi supaya user memulai dari layar timer.
                val open = Intent(this, MainActivity::class.java)
                open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    startActivity(open)
                } catch (_: Exception) {
                    // tidak ada yang bisa dilakukan
                }
            }
        }
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val running = Prefs.isTimerRunning(this)
        if (running) {
            tile.state = Tile.STATE_ACTIVE
            val remaining =
                (Prefs.timerEndElapsed(this) - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            tile.subtitle = SleepTimerService.formatDuration(remaining)
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.subtitle = getString(R.string.tile_sleep_timer_subtitle_off)
        }
        tile.updateTile()
    }
}
