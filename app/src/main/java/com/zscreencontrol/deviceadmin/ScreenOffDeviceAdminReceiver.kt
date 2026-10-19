package com.zscreencontrol.deviceadmin

import android.app.admin.DeviceAdminReceiver

/**
 * Device Admin minimal: satu-satunya kebijakan yang diminta adalah force-lock
 * (lihat res/xml/device_admin.xml). Dipakai oleh core/ScreenOffController.
 */
class ScreenOffDeviceAdminReceiver : DeviceAdminReceiver()
