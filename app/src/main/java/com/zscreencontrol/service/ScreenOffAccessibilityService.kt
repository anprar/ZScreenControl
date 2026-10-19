package com.zscreencontrol.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Layanan aksesibilitas ZScreenControl.
 *
 * v0.1: dipakai untuk GLOBAL_ACTION_LOCK_SCREEN (kunci layar tanpa Device Admin).
 * v0.2 (rencana): memantau perpindahan aplikasi (TYPE_WINDOW_STATE_CHANGED)
 * untuk fitur timeout per aplikasi, seperti pada referensi Screen Suite.
 */
class ScreenOffAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // v0.2: deteksi app foreground di sini (debounce ~500 ms).
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    /** Kunci layar langsung. @return false jika tidak didukung/gagal. */
    fun lockScreen(): Boolean = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)

    companion object {
        /** Instance aktif; null berarti layanan tidak berjalan. */
        @Volatile
        var instance: ScreenOffAccessibilityService? = null
            private set
    }
}
