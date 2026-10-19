package com.zscreencontrol.tile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.zscreencontrol.MainActivity
import com.zscreencontrol.core.OffMethod
import com.zscreencontrol.core.ScreenOffController

/**
 * Quick Settings Tile: ketuk untuk langsung mematikan/mengunci layar.
 * Untuk aksi ini sistem mengizinkan startActivity dari tile,
 * jadi kalau belum ada izin kita buka aplikasinya.
 */
class ScreenOffTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val available = ScreenOffController.availableMethod(this) != OffMethod.NONE
        qsTile?.apply {
            state = if (available) Tile.STATE_INACTIVE else Tile.STATE_UNAVAILABLE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        if (ScreenOffController.turnOff(this) == OffMethod.NONE) {
            openApp()
        }
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            val pi = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pi)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
