package com.zscreencontrol.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Pembantu untuk perangkat yang agresif membunuh background service.
 * Kasus utama aplikasi ini: OPPO A5 2020 (ColorOS) dan keluarga OPPO/Realme/OnePlus.
 */
object BatteryCompat {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Buka daftar optimasi baterai.
     * Sengaja memakai halaman daftar (bukan dialog per-app via
     * REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) supaya tidak butuh izin sensitif
     * yang dibatasi kebijakan Play Store.
     */
    fun batterySettingsIntent(): Intent =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    fun isOppoFamily(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return manufacturer.contains("oppo") || manufacturer.contains("realme") ||
            manufacturer.contains("oneplus") || brand.contains("oppo") ||
            brand.contains("realme") || brand.contains("oneplus")
    }

    private val AUTO_START_COMPONENTS = listOf(
        ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
        ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
        ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
        ComponentName("com.oplus.safecenter", "com.oplus.safecenter.startupapp.StartupAppListActivity")
    )

    /**
     * Coba buka halaman auto-start ColorOS.
     * Memakai startActivity + try/catch (bukan resolveActivity) supaya tidak
     * terhalang pembatasan package visibility di Android 11+.
     * @return true jika salah satu intent berhasil dibuka.
     */
    fun openAutoStartSettings(context: Context): Boolean {
        for (component in AUTO_START_COMPONENTS) {
            try {
                context.startActivity(
                    Intent().setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return true
            } catch (_: Exception) {
                // coba kandidat berikutnya
            }
        }
        return false
    }

    fun appDetailsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
}
