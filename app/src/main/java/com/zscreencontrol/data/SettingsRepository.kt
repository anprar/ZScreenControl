package com.zscreencontrol.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Kunci & default setelan aplikasi. */
object SettingKeys {
    const val METHOD_AUTO = "auto"
    const val METHOD_DEVICE_ADMIN = "device_admin"
    const val METHOD_ACCESSIBILITY = "accessibility"
    const val METHOD_OVERLAY = "overlay"

    val OFF_METHOD = stringPreferencesKey("off_method")

    val DEFAULTS = mapOf(
        OFF_METHOD.name to METHOD_AUTO
    )
}

/**
 * Setelan aplikasi berbasis DataStore (upgrade dari SharedPreferences).
 * Semua akses melewati repository ini agar kontraknya tunggal.
 */
class SettingsRepository(private val context: Context) {

    /** Metode mematikan layar yang dipilih user ("auto" = pilih otomatis). */
    val offMethod: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SettingKeys.OFF_METHOD] ?: SettingKeys.METHOD_AUTO
    }

    /** Baca langsung (sinkron) — hanya untuk pemanggilan dari service/tile di thread kerja. */
    suspend fun offMethodOnce(): String =
        context.dataStore.data.first()[SettingKeys.OFF_METHOD] ?: SettingKeys.METHOD_AUTO

    suspend fun setOffMethod(method: String) {
        context.dataStore.edit { prefs ->
            prefs[SettingKeys.OFF_METHOD] = method
        }
    }
}
