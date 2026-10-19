package com.zscreencontrol.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.zscreencontrol.R
import com.zscreencontrol.core.Permissions
import com.zscreencontrol.core.Prefs
import com.zscreencontrol.service.SleepTimerService
import kotlinx.coroutines.delay

/**
 * Tab 2 — Sleep Timer:
 * pilih durasi -> foreground service menghitung mundur -> layar dimatikan
 * otomatis lewat ScreenOffController saat waktu habis.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SleepTimerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var selectedMinutes by rememberSaveable { mutableStateOf(Prefs.lastTimerMinutes(context)) }
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

    // Ticker 1 detik: UI membaca status dari Prefs, jadi selalu sinkron dengan service.
    LaunchedEffect(Unit) {
        while (true) {
            now = SystemClock.elapsedRealtime()
            delay(1_000L)
        }
    }

    val running = Prefs.isTimerRunning(context)
    val remainingMs = if (running) {
        (Prefs.timerEndElapsed(context) - now).coerceAtLeast(0L)
    } else {
        0L
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startSleepTimer(context, selectedMinutes)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (running) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.sleep_remaining),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        SleepTimerService.formatDuration(remainingMs),
                        style = MaterialTheme.typography.displayMedium
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = { extendSleepTimer(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.sleep_extend))
                }
                Button(
                    onClick = { stopSleepTimer(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.sleep_stop))
                }
            }
        } else {
            Text(
                stringResource(R.string.sleep_presets),
                style = MaterialTheme.typography.titleSmall
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SLEEP_PRESETS_MINUTES.forEach { minutes ->
                    FilterChip(
                        selected = selectedMinutes == minutes,
                        onClick = { selectedMinutes = minutes },
                        label = { Text(stringResource(R.string.sleep_minutes_value, minutes)) }
                    )
                }
            }

            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= 33 && !Permissions.hasNotificationPermission(context)) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        startSleepTimer(context, selectedMinutes)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.sleep_start))
            }

            Text(
                stringResource(R.string.sleep_idle_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val SLEEP_PRESETS_MINUTES = listOf(5, 10, 15, 30, 45, 60, 90, 120)

private fun startSleepTimer(context: Context, minutes: Int) {
    ContextCompat.startForegroundService(
        context,
        Intent(context, SleepTimerService::class.java)
            .setAction(SleepTimerService.ACTION_START)
            .putExtra(SleepTimerService.EXTRA_MINUTES, minutes)
    )
}

private fun stopSleepTimer(context: Context) {
    context.startService(
        Intent(context, SleepTimerService::class.java)
            .setAction(SleepTimerService.ACTION_STOP)
    )
}

private fun extendSleepTimer(context: Context) {
    context.startService(
        Intent(context, SleepTimerService::class.java)
            .setAction(SleepTimerService.ACTION_EXTEND)
    )
}
