package com.zscreencontrol.ui.screen

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.zscreencontrol.core.Permissions
import com.zscreencontrol.data.db.AppDatabase
import com.zscreencontrol.data.db.AppTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Tab 4 — Timeout per aplikasi (v0.2).
 * Daftar app terdaftar (Room) beserta batasnya; tombol "Tambah aplikasi"
 * memakai dialog picker (app launcher terpasang), lalu dialog durasi.
 *
 * Mekanismenya dikelola [com.zscreencontrol.service.ScreenOffAccessibilityService]
 * — kartu pengingat tampil bila layanan aksesibilitas belum aktif.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppTimeoutListScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.get(context).appTimeoutDao() }
    val registered by dao.observeAll().collectAsState(initial = emptyList())

    // Segarkan status izin saat kembali ke layar ini.
    var refreshKey by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val accessibilityOn = remember(refreshKey) {
        Permissions.isAccessibilityServiceEnabled(context)
    }

    var pickerVisible by remember { mutableStateOf(false) }
    var pendingPick by remember { mutableStateOf<PendingPick?>(null) }
    var editTarget by remember { mutableStateOf<AppTimeout?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.applist_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                stringResource(R.string.applist_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!accessibilityOn) {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.applist_accessibility_needed),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    FilledTonalButton(onClick = {
                        context.startActivity(Permissions.accessibilitySettingsIntent())
                    }) {
                        Text(stringResource(R.string.applist_accessibility_grant))
                    }
                }
            }
        }

        if (registered.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.applist_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(registered, key = { it.packageName }) { item ->
                    RegisteredRow(
                        item = item,
                        onEdit = { editTarget = item },
                        onDelete = { scope.launch { dao.delete(item.packageName) } }
                    )
                }
            }
        }

        Button(
            onClick = { pickerVisible = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.applist_add))
        }
    }

    // Pilih aplikasi (daftar app launcher terpasang).
    if (pickerVisible) {
        AppPickerDialog(
            context = context,
            onDismiss = { pickerVisible = false },
            onPick = { entry ->
                pickerVisible = false
                pendingPick = PendingPick(entry.packageName, entry.label)
            }
        )
    }

    // Pilih durasi untuk app yang baru ditambahkan.
    pendingPick?.let { pick ->
        TimeoutPickerDialog(
            appName = pick.label,
            initialSeconds = 300,
            onDismiss = { pendingPick = null },
            onSave = { seconds ->
                scope.launch {
                    dao.upsert(AppTimeout(pick.packageName, pick.label, seconds))
                }
                pendingPick = null
            }
        )
    }

    // Ubah durasi app yang sudah terdaftar.
    editTarget?.let { target ->
        TimeoutPickerDialog(
            appName = target.appName,
            initialSeconds = target.timeoutSeconds,
            onDismiss = { editTarget = null },
            onSave = { seconds ->
                scope.launch {
                    dao.upsert(target.copy(timeoutSeconds = seconds))
                }
                editTarget = null
            }
        )
    }
}

private data class PendingPick(val packageName: String, val label: String)

@Composable
private fun RegisteredRow(
    item: AppTimeout,
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
            Column(Modifier.weight(1f)) {
                Text(item.appName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    item.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = onEdit) {
                Text(formatTimeoutLabel(item.timeoutSeconds))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, stringResource(R.string.applist_delete_desc))
            }
        }
    }
}

/** Dialog pilih aplikasi: daftar app launcher + pencarian. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppPickerDialog(
    context: Context,
    onDismiss: () -> Unit,
    onPick: (AppEntry) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val apps: List<AppEntry> by produceState(initialValue = emptyList()) {
        value = withContext(Dispatchers.IO) { loadAppsSortedByUsage(context) }
    }
    val filtered = apps.filter {
        it.label.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.applist_picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.applist_search_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(filtered, key = { it.packageName }) { app ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(app) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(app.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val lastUsed = formatLastUsed(app.lastUsedSecondsAgo)
                            if (lastUsed.isNotEmpty()) {
                                Text(
                                    lastUsed,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.applist_picker_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

/** Dialog pilih durasi timeout (menit/jam). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeoutPickerDialog(
    appName: String,
    initialSeconds: Int = 300,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    val options = listOf(60, 180, 300, 600, 900, 1800, 3600)
    var selected by remember {
        mutableIntStateOf(options.indexOf(initialSeconds).takeIf { it >= 0 } ?: 1)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(appName) },
        text = {
            Column {
                Text(stringResource(R.string.applist_pick_timeout))
                Spacer(Modifier.padding(4.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    options.forEachIndexed { index, seconds ->
                        FilledTonalButton(
                            onClick = { selected = index }
                        ) {
                            Text(
                                if (selected == index) {
                                    "• " + formatTimeoutLabel(seconds)
                                } else {
                                    formatTimeoutLabel(seconds)
                                }
                            )
                        }
                    }
                }
                Text(
                    stringResource(R.string.applist_pick_timeout_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(options[selected]) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

internal data class AppEntry(
    val packageName: String,
    val label: String,
    val lastUsedSecondsAgo: Int
)

/** Label durasi: detik -> "1 menit" / "1 jam 30 mnt" / "30 detik". */
internal fun formatTimeoutLabel(seconds: Int): String = when {
    seconds >= 3600 -> {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        if (minutes > 0) "${hours} jam $minutes mnt" else "$hours jam"
    }
    seconds >= 60 -> "${seconds / 60} menit"
    else -> "$seconds detik"
}

/** Desripsi kapan app terakhir dipakai (untuk pengurutan tampilan). */
private fun formatLastUsed(secondsAgo: Int): String = when {
    secondsAgo < 0 -> ""
    secondsAgo < 60 -> "baru saja"
    secondsAgo < 3600 -> "${secondsAgo / 60} mnt lalu"
    secondsAgo < 86400 -> "${secondsAgo / 3600} jam lalu"
    else -> "${secondsAgo / 86400} hari lalu"
}

/**
 * Baca daftar aplikasi launchable; kalau ada izin "Usage access",
 * urutkan dari yang paling baru dipakai, kalau tidak urut abjad.
 */
private fun loadAppsSortedByUsage(context: Context): List<AppEntry> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val launchables = pm.queryIntentActivities(intent, 0)

    val byPackage = LinkedHashMap<String, AppEntry>()
    for (info in launchables) {
        val pkg = info.activityInfo.packageName
        if (pkg == context.packageName) continue
        if (byPackage.containsKey(pkg)) continue
        byPackage[pkg] = AppEntry(
            packageName = pkg,
            label = info.loadLabel(pm).toString(),
            lastUsedSecondsAgo = -1
        )
    }

    val hasUsage = Permissions.hasUsageAccess(context)
    if (hasUsage) {
        val usage = runCatching {
            context.getSystemService(UsageStatsManager::class.java)
        }.getOrNull()
        if (usage != null) {
            val now = System.currentTimeMillis()
            val stats: List<UsageStats> = usage.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                now - 30L * 86_400_000L,
                now
            )
            // Perbarui entry terakhir yang dipakai (pertahankan yang terbaru).
            for (s in stats) {
                if (s.lastTimeUsed <= 0L) continue
                val existing = byPackage[s.packageName] ?: continue
                val ago = ((now - s.lastTimeUsed) / 1000L).toInt()
                val better = existing.lastUsedSecondsAgo < 0 || ago < existing.lastUsedSecondsAgo
                if (better) {
                    byPackage[s.packageName] = existing.copy(lastUsedSecondsAgo = ago)
                }
            }
        }
    }

    return byPackage.values.toList().let { list ->
        if (hasUsage) {
            list.sortedWith(
                compareBy({ it.lastUsedSecondsAgo < 0 }, { it.lastUsedSecondsAgo })
            )
        } else {
            list.sortedBy { it.label.lowercase() }
        }
    }
}
