package com.zscreencontrol

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class ZScreenApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    CHANNEL_TIMER,
                    getString(R.string.channel_timer_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = getString(R.string.channel_timer_desc) },
                NotificationChannel(
                    CHANNEL_OVERLAY,
                    getString(R.string.channel_overlay_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = getString(R.string.channel_overlay_desc) }
            )
        )
    }

    companion object {
        const val CHANNEL_TIMER = "sleep_timer"
        const val CHANNEL_OVERLAY = "black_overlay"
    }
}
