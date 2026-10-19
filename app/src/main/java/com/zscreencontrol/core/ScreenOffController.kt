package com.zscreencontrol.core

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.zscreencontrol.deviceadmin.ScreenOffDeviceAdminReceiver
import com.zscreencontrol.service.BlackOverlayService
import com.zscreencontrol.service.ScreenOffAccessibilityService

enum class OffMethod { DEVICE_ADMIN, ACCESSIBILITY, OVERLAY, NONE }

/**
 * Lapisan "matikan layar" dengan fallback bertingkat:
 *  1. Device Admin  -> lockNow()            (kunci layar sungguhan)
 *  2. Accessibility -> GLOBAL_ACTION_LOCK_SCREEN (API 28+)
 *  3. Overlay hitam -> screen-off palsu     (layar fisik tetap menyala)
 *
 * Urutan ini mengikuti pelajaran dari hasil analisis Screen Suite:
 * metode paling andal yang tersedia dipakai lebih dulu, overlay jadi jaring pengaman.
 */
object ScreenOffController {

    fun availableMethod(context: Context): OffMethod = when {
        Permissions.isDeviceAdminActive(context) -> OffMethod.DEVICE_ADMIN
        Permissions.isAccessibilityServiceEnabled(context) -> OffMethod.ACCESSIBILITY
        Permissions.canDrawOverlays(context) -> OffMethod.OVERLAY
        else -> OffMethod.NONE
    }

    /** Matikan layar sekarang. @return metode yang benar-benar dipakai. */
    fun turnOff(context: Context): OffMethod {
        return when (availableMethod(context)) {
            OffMethod.DEVICE_ADMIN -> {
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                val admin = ComponentName(context, ScreenOffDeviceAdminReceiver::class.java)
                if (dpm != null && dpm.isAdminActive(admin)) {
                    try {
                        dpm.lockNow()
                        OffMethod.DEVICE_ADMIN
                    } catch (_: SecurityException) {
                        fallback(context)
                    }
                } else {
                    fallback(context)
                }
            }

            OffMethod.ACCESSIBILITY -> {
                val locked = ScreenOffAccessibilityService.instance?.lockScreen() ?: false
                if (locked) OffMethod.ACCESSIBILITY else fallback(context)
            }

            OffMethod.OVERLAY -> {
                showOverlay(context)
                OffMethod.OVERLAY
            }

            OffMethod.NONE -> OffMethod.NONE
        }
    }

    private fun fallback(context: Context): OffMethod =
        if (Permissions.canDrawOverlays(context)) {
            showOverlay(context)
            OffMethod.OVERLAY
        } else {
            OffMethod.NONE
        }

    private fun showOverlay(context: Context) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, BlackOverlayService::class.java)
        )
    }
}
