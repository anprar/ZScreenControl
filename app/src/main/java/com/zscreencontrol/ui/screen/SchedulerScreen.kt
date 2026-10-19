package com.zscreencontrol.ui.screen

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zscreencontrol.R
import com.zscreencontrol.data.db.AppDatabase
import com.zscreencontrol.data.db.Schedule
import com.zscreencontrol.data.db.SchedulerConstants
import com.zscreencontrol.scheduler.SleepScheduler
import kotlinx.coroutines.launch

/**
 * Tab 3 — Jadwal (v0.2):
 * daftar jadwal (Room) + editor jam/menit/hari; AlarmManager
 * dipasang ulang setiap daftar berubah oleh [SleepScheduler].
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SchedulerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.get(context).scheduleDao() }
    val schedules by dao.observeAll().collectAsState(initial = emptyList())

    var editorVisible by rememberSaveable { mutableStateOf(false) }
    var editorSchedule by remember { mutableStateOf<Schedule?>(null) }

    // Status "Alarm & pengingat" (SCHEDULE_EXACT_ALARM) — wajib diizinkan
    // khususnya di ColorOS; tanpa itu jadwal bisa telat.
    var refreshKey by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val exactOkay = remember(refreshKey) {
        if (Build.VERSION.SDK_INT < 31) true else canScheduleExactAlarms(context)
    }

    // Pasang ulang alarm tiap daftar berubah (tambah/hapus/ubah/aktif).
    LaunchedEffect(schedules) {
        SleepScheduler.scheduleNext(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(R.string.scheduler_title),
            style = MaterialTheme.typography.titleLarge
        )

        if (Build.VERSION.SDK_INT >= 31 && !exactOkay) {
            ExactAlarmHintCard(
                onGrant = {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    )
                }
            )
        }

        if (schedules.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        stringResource(R.string.scheduler_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(schedules, key = { it.id }) { schedule ->
                    ScheduleRow(
                        schedule = schedule,
                        onToggle = { enabled ->
                            scope.launch { dao.setEnabled(schedule.id, enabled) }
                        },
                        onEdit = {
                            editorSchedule = schedule
                            editorVisible = true
                        },
                        onDelete = {
                            scope.launch { dao.delete(schedule) }
                        }
                    )
                }
            }
        }

        Button(
            onClick = {
                editorSchedule = null
                editorVisible = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.scheduler_add))
        }
    }

    if (editorVisible) {
        ScheduleEditorDialog(
            schedule = editorSchedule,
            onDismiss = { editorVisible = false },
            onSave = { hour, minute, mask ->
                val target = editorSchedule
                scope.launch {
                    if (target != null) {
                        dao.update(target.copy(hour = hour, minute = minute, daysMask = mask))
                    } else {
                        dao.upsert(Schedule(hour = hour, minute = minute, daysMask = mask))
                    }
                }
                editorVisible = false
            }
        )
    }
}

@Composable
private fun ScheduleRow(
    schedule: Schedule,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .clickable { onEdit() }
            ) {
                Text(
                    "%02d:%02d".format(schedule.hour, schedule.minute),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    SleepScheduler.daysMaskToLabel(schedule.daysMask)
                        ?: stringResource(R.string.scheduler_days_all_default),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onEdit) {
                Text(stringResource(R.string.scheduler_edit))
            }
            Switch(checked = schedule.enabled, onCheckedChange = onToggle)
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    stringResource(R.string.scheduler_delete_desc)
                )
            }
        }
    }
}

/** Peringatan izin alarm tepat (API 31+). */
@Composable
private fun ExactAlarmHintCard(onGrant: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.scheduler_exact_title),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    stringResource(R.string.scheduler_exact_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = onGrant) {
                Text(stringResource(R.string.action_grant))
            }
        }
    }
}

/** Dialog tambah/ubah jadwal: jam, menit, hari (chip). */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleEditorDialog(
    schedule: Schedule?,
    onDismiss: () -> Unit,
    onSave: (hour: Int, minute: Int, daysMask: Int) -> Unit
) {
    var hour by remember { mutableIntStateOf(schedule?.hour ?: 22) }
    var minute by remember { mutableIntStateOf(schedule?.minute ?: 0) }
    var mask by remember { mutableIntStateOf(schedule?.daysMask ?: SchedulerConstants.DAYS_ALL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (schedule == null) R.string.scheduler_add_title
                    else R.string.scheduler_edit_title
                )
            )
        },
        text = {
            Column(
                Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.scheduler_hour_label), style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0..23).forEach { h ->
                        FilterChip(
                            selected = hour == h,
                            onClick = { hour = h },
                            label = { Text("%02d".format(h)) }
                        )
                    }
                }

                Text(stringResource(R.string.scheduler_minute_label), style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SCHEDULE_MINUTE_STEPS.forEach { m ->
                        FilterChip(
                            selected = minute == m,
                            onClick = { minute = m },
                            label = { Text("%02d".format(m)) }
                        )
                    }
                }

                Text(stringResource(R.string.scheduler_days_label), style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DAY_LABELS.forEach { (label, bit) ->
                        FilterChip(
                            selected = mask and bit != 0,
                            onClick = { mask = mask xor bit },
                            label = { Text(label) }
                        )
                    }
                }

                if (mask == 0) {
                    Text(
                        stringResource(R.string.scheduler_days_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = mask != 0,
                onClick = { onSave(hour, minute, mask) }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

private fun canScheduleExactAlarms(context: Context): Boolean =
    (context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)
        ?.canScheduleExactAlarms() ?: false

/** Label hari Sen..Min, pas dengan bit [SchedulerConstants.DAY_MON..DAY_SUN]. */
private val DAY_LABELS = listOf(
    "Sen" to SchedulerConstants.DAY_MON,
    "Sel" to SchedulerConstants.DAY_TUE,
    "Rab" to SchedulerConstants.DAY_WED,
    "Kam" to SchedulerConstants.DAY_THU,
    "Jum" to SchedulerConstants.DAY_FRI,
    "Sab" to SchedulerConstants.DAY_SAT,
    "Min" to SchedulerConstants.DAY_SUN
)

private val SCHEDULE_MINUTE_STEPS = listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)
