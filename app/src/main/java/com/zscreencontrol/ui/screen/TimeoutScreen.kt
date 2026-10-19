package com.zscreencontrol.ui.screen

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zscreencontrol.R
import com.zscreencontrol.core.AppGraph
import com.zscreencontrol.core.BatteryCompat
import com.zscreencontrol.core.OffMethod
import com.zscreencontrol.core.Permissions
import com.zscreencontrol.core.ScreenOffController
import com.zscreencontrol.data.SettingKeys
import kotlinx.coroutines.launch

/**
 * Tab 1 — Timeout Layar:
 * - ubah SCREEN_OFF_TIMEOUT sistem (butuh WRITE_SETTINGS)
 * - tombol "matikan layar sekarang" (fallback bertingkat)
 * - daftar status izin + panduan khusus ColorOS/OPPO
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TimeoutScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val settings = remember { AppGraph.settings }
    val offMethodKey by settings.offMethod.collectAsState(initial = SettingKeys.METHOD_AUTO)

    // Segarkan status setiap kali layar kembali aktif (mis. setelah balik dari
    // halaman Settings pemberian izin).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val currentTimeoutMs = remember(refreshKey) { readSystemTimeoutMs(context) }
    val method = remember(refreshKey) { ScreenOffController.availableMethod(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    stringResource(R.string.timeout_current),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    formatTimeout(currentTimeoutMs),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.timeout_presets), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TIMEOUT_PRESETS_MS.forEach { presetMs ->
                    AssistChip(
                        onClick = {
                            if (!Permissions.canWriteSettings(context)) {
                                context.startActivity(Permissions.writeSettingsIntent(context))
                            } else if (applySystemTimeout(context, presetMs)) {
                                refreshKey++
                            }
                        },
                        label = { Text(formatTimeout(presetMs)) }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    ScreenOffController.turnOff(context)
                    refreshKey++
                },
                enabled = method != OffMethod.NONE,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.timeout_turn_off_now))
            }
            Text(
                text = methodLabel(method),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.method_section_title),
                style = MaterialTheme.typography.titleSmall
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                METHOD_OPTIONS.forEach { (key, labelRes) ->
                    FilterChip(
                        selected = offMethodKey == key,
                        onClick = { scope.launch { settings.setOffMethod(key) } },
                        label = { Text(stringResource(labelRes)) }
                    )
                }
            }
            Text(
                stringResource(R.string.method_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.perm_section_title),
                style = MaterialTheme.typography.titleSmall
            )

            PermissionRow(
                title = stringResource(R.string.perm_write_settings),
                description = stringResource(R.string.perm_write_settings_desc),
                active = Permissions.canWriteSettings(context),
                actionLabel = stringResource(R.string.action_grant),
                onAction = { context.startActivity(Permissions.writeSettingsIntent(context)) }
            )

            PermissionRow(
                title = stringResource(R.string.perm_overlay),
                description = stringResource(R.string.perm_overlay_desc),
                active = Permissions.canDrawOverlays(context),
                actionLabel = stringResource(R.string.action_grant),
                onAction = { context.startActivity(Permissions.overlaySettingsIntent(context)) }
            )

            PermissionRow(
                title = stringResource(R.string.perm_accessibility),
                description = stringResource(R.string.perm_accessibility_desc),
                active = Permissions.isAccessibilityServiceEnabled(context),
                actionLabel = stringResource(R.string.action_open),
                onAction = { context.startActivity(Permissions.accessibilitySettingsIntent()) }
            )

            PermissionRow(
                title = stringResource(R.string.perm_device_admin),
                description = stringResource(R.string.perm_device_admin_desc),
                active = Permissions.isDeviceAdminActive(context),
                actionLabel = stringResource(R.string.action_grant),
                onAction = { context.startActivity(Permissions.deviceAdminRequestIntent(context)) }
            )

            PermissionRow(
                title = stringResource(R.string.perm_battery),
                description = stringResource(R.string.perm_battery_desc),
                active = BatteryCompat.isIgnoringBatteryOptimizations(context),
                actionLabel = stringResource(R.string.action_open),
                onAction = { context.startActivity(BatteryCompat.batterySettingsIntent()) }
            )

            if (BatteryCompat.isOppoFamily()) {
                PermissionRow(
                    title = stringResource(R.string.perm_autostart),
                    description = stringResource(R.string.perm_autostart_desc),
                    active = null,
                    actionLabel = stringResource(R.string.action_open),
                    onAction = {
                        if (!BatteryCompat.openAutoStartSettings(context)) {
                            context.startActivity(BatteryCompat.appDetailsIntent(context))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    active: Boolean?,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        when (active) {
            true -> Text(
                stringResource(R.string.perm_status_active),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )

            false -> FilledTonalButton(onClick = onAction) { Text(actionLabel) }
            null -> OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun methodLabel(method: OffMethod): String = when (method) {
    OffMethod.DEVICE_ADMIN -> stringResource(R.string.timeout_method_device_admin)
    OffMethod.ACCESSIBILITY -> stringResource(R.string.timeout_method_accessibility)
    OffMethod.OVERLAY -> stringResource(R.string.timeout_method_overlay)
    OffMethod.NONE -> stringResource(R.string.timeout_method_none)
}

@Composable
private fun formatTimeout(ms: Int): String =
    if (ms > 0 && ms % 60_000 == 0) {
        stringResource(R.string.timeout_minutes, ms / 60_000)
    } else {
        stringResource(R.string.timeout_seconds, ms / 1000)
    }

private val TIMEOUT_PRESETS_MS = listOf(
    15_000, 30_000, 60_000, 120_000, 300_000, 600_000, 1_800_000
)

/** Pilihan metode mematikan layar (disimpan di DataStore). */
private val METHOD_OPTIONS = listOf(
    SettingKeys.METHOD_AUTO to R.string.method_auto,
    SettingKeys.METHOD_DEVICE_ADMIN to R.string.method_device_admin,
    SettingKeys.METHOD_ACCESSIBILITY to R.string.method_accessibility,
    SettingKeys.METHOD_OVERLAY to R.string.method_overlay
)

private fun readSystemTimeoutMs(context: Context): Int = try {
    Settings.System.getInt(
        context.contentResolver,
        Settings.System.SCREEN_OFF_TIMEOUT,
        30_000
    )
} catch (_: Exception) {
    30_000
}

private fun applySystemTimeout(context: Context, ms: Int): Boolean = try {
    Settings.System.putInt(
        context.contentResolver,
        Settings.System.SCREEN_OFF_TIMEOUT,
        ms
    )
} catch (_: Exception) {
    false
}
