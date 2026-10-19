package com.zscreencontrol.core

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.zscreencontrol.data.SettingKeys
import com.zscreencontrol.data.SettingsRepository

/**
 * Penyedia dependensi minimal (tanpa framework DI).
 * Diinisialisasi di ZScreenApp.onCreate().
 *
 * offMethodCache: metode mematikan layar pilihan user (DataStore) di-cache di
 * memori agar bisa dibaca dari service/receiver tanpa coroutine
 * (ScreenOffController).
 */
object AppGraph {
    @Volatile
    private var appContext: Context? = null

    /** Scope milik proses untuk pekerjaan latar (koleksi flow, dsb). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings: SettingsRepository by lazy {
        SettingsRepository(requireAppContext())
    }

    @Volatile
    var offMethodCache: String = SettingKeys.METHOD_AUTO
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        // Baca awal secara sinkron (kilat saja) supaya receiver widget/alarm
        // tidak memakai default salah saat proses baru dibangunkan sistem.
        runBlocking(Dispatchers.IO) {
            offMethodCache = try {
                settings.offMethodOnce()
            } catch (_: Exception) {
                SettingKeys.METHOD_AUTO
            }
        }
        appScope.launch {
            settings.offMethod.collect { offMethodCache = it }
        }
    }

    fun appContext(): Context = requireAppContext()

    private fun requireAppContext(): Context =
        appContext ?: error("AppGraph.init() belum dipanggil (ZScreenApp.onCreate)")
}
