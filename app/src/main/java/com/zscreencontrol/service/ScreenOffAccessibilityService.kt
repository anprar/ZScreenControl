package com.zscreencontrol.service

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.zscreencontrol.core.ScreenOffController
import com.zscreencontrol.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Layanan aksesibilitas ZScreenControl:
 *
 * 1. Kunci layar: GLOBAL_ACTION_LOCK_SCREEN (dipakai ScreenOffController).
 * 2. Timeout per aplikasi: memantau TYPE_WINDOW_STATE_CHANGED; kalau app
 *    terdaftar di daftar per-app masuk foreground, hitung mundur sesuai
 *    nilai timeout-nya. Pindah ke app lain (tidak terdaftar) -> reset.
 *
 * Daftar app & timeout dimuat via Flow (Room) dan disimpan di memori,
 * jadi event handler berjalan tanpa akses DB (hemat & tidak membuat
 * jank saat transisi activity).
 */
class ScreenOffAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val timeouts = ConcurrentHashMap<String, Int>()

    private var activePackage: String? = null
    private var pendingFire: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        scope.launch {
            AppDatabase.get(applicationContext).appTimeoutDao().observeAll()
                .collect { list ->
                    timeouts.clear()
                    list.forEach { timeouts[it.packageName] = it.timeoutSeconds }
                }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Hanya peduli perpindahan window.
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (isIgnored(pkg)) return

        val seconds = timeouts[pkg]

        if (seconds == null) {
            // App tidak terdaftar -> batalkan countdown yang mungkin berjalan.
            cancelPending()
            activePackage = null
            return
        }

        // App yang sama sedang aktif -> biarkan countdown berjalan
        // (jangan di-reset setiap ganti activity di app yang sama).
        if (pkg == activePackage) return

        // Pindah ke app terdaftar (atau lain) -> mulai countdown baru.
        cancelPending()
        activePackage = pkg
        val fire = Runnable {
            if (activePackage == pkg && timeouts.containsKey(pkg)) {
                ScreenOffController.turnOff(applicationContext)
                activePackage = null
            }
        }
        pendingFire = fire
        handler.postDelayed(fire, seconds.coerceAtLeast(1) * 1000L)
    }

    private fun cancelPending() {
        pendingFire?.let { handler.removeCallbacks(it) }
        pendingFire = null
    }

    /**
     * Paket yang melompat-lompat (sistem, keyboard, aplikasi sendiri)
     * tidak boleh dianggap "pindah aplikasi".
     */
    private fun isIgnored(pkg: String): Boolean {
        if (pkg == packageName) return true
        if (pkg == "android" || pkg == "com.android.systemui") return true
        // Keyboard (IME): layar window milik keyboard muncul saat mengetik.
        if (pkg.contains("inputmethod") || pkg.contains("keyboard") || pkg.contains("swiftkey")) return true
        return false
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        cancelPending()
        scope.cancel()
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
