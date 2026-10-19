package com.zscreencontrol.core

import android.content.Context
import android.content.SharedPreferences

/**
 * Penyimpanan setelan sederhana (v0.1).
 * Sumber kebenaran status sleep timer: user menyimpan waktu mulai/akhir di sini
 * sehingga UI dan service selalu sinkron walau proses sempat dimatikan sistem.
 */
object Prefs {

    private const val NAME = "settings"
    private const val KEY_TIMER_RUNNING = "timer_running"
    private const val KEY_TIMER_END = "timer_end_elapsed"
    private const val KEY_TIMER_DURATION = "timer_duration_ms"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun isTimerRunning(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TIMER_RUNNING, false)

    fun timerEndElapsed(context: Context): Long =
        prefs(context).getLong(KEY_TIMER_END, 0L)

    fun timerDurationMs(context: Context): Long =
        prefs(context).getLong(KEY_TIMER_DURATION, 0L)

    fun setTimer(context: Context, durationMs: Long, endElapsedRealtime: Long) {
        prefs(context).edit()
            .putBoolean(KEY_TIMER_RUNNING, true)
            .putLong(KEY_TIMER_END, endElapsedRealtime)
            .putLong(KEY_TIMER_DURATION, durationMs)
            .apply()
    }

    fun clearTimer(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_TIMER_RUNNING, false)
            .apply()
    }
}
