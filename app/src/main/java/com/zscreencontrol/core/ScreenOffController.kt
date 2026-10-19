package com.zscreencontrol.core

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.zscreencontrol.data.SettingKeys
import com.zscreencontrol.deviceadmin.ScreenOffDeviceAdminReceiver
import com.zscreencontrol.service.BlackOverlayService
import com.zscreencontrol.service.ScreenOffAccessibilityService

enum class OffMethod { DEVICE_ADMIN, ACCESSIBILITY, OVERLAY, NONE }

/**
 * Lapisan "matikan layar" dengan dua mode pilihan:
 *
 * - AUTO (default): fallback bertingkat —
 *     1. Device Admin  -> lockNow()
 *     2. Accessibility -> GLOBAL_ACTION_LOCK_SCREEN (API 28+)
 *     3. Overlay hitam -> screen-off palsu (layar fisik tetap menyala)
 * - Eksplisit: user memilih salah satu metode di pengaturan; metode lain
 *   dipakai hanya sebagai jaring pengaman bila metode pilihan gagal dipanggil.
 */
object ScreenOffController {

    /** Metode yang tersedia saat ini (abaikan preferensi user). */
    fun availableMethod(context: Context): OffMethod = when {
        Permissions.isDeviceAdminActive(context) -> OffMethod.DEVICE_ADMIN
        Permissions.isAccessibilityServiceEnabled(context) -> OffMethod.ACCESSIBILITY
        Permissions.canDrawOverlays(context) -> OffMethod.OVERLAY
        else -> OffMethod.NONE
    }

    /** Matikan layar sesuai preferensi user ("auto" = fallback bertingkat). */
    fun turnOff(context: Context): OffMethod {
        return when (AppGraph.offMethodCache) {
            SettingKeys.METHOD_DEVICE_ADMIN -> deviceAdminOff(context)
            SettingKeys.METHOD_ACCESSIBILITY -> accessibilityOff(context)
            SettingKeys.METHOD_OVERLAY -> overlayOff(context)
            else -> autoOff(context)
        }
    }

    private fun autoOff(context: Context): OffMethod =
        deviceAdminOff(context)
            .takeIf { it != OffMethod.NONE }
            ?: accessibilityOff(context).takeIf { it != OffMethod.NONE }
            ?: overlayOff(context)

    private fun deviceAdminOff(context: Context): OffMethod {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val admin = ComponentName(context, ScreenOffDeviceAdminReceiver::class.java)
        if (dpm == null || !dpm.isAdminActive(admin)) return OffMethod.NONE
        return try {
            dpm.lockNow()
            OffMethod.DEVICE_ADMIN
        } catch (_: SecurityException) {
            OffMethod.NONE
        }
    }

    private fun accessibilityOff(context: Context): OffMethod {
        val service = ScreenOffAccessibilityService.instance ?: return OffMethod.NONE
        return if (service.lockScreen()) OffMethod.ACCESSIBILITY else OffMethod.NONE
    }

    private fun overlayOff(context: Context): OffMethod {
        if (!Permissions.canDrawOverlays(context)) return OffMethod.NONE
        return try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, BlackOverlayService::class.java)
            )
            OffMethod.OVERLAY
        } catch (_: Exception) {
            OffMethod.NONE
        }
    }
}
