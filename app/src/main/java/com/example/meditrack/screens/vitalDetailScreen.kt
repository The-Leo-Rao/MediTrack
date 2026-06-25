package com.example.meditrack.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.meditrack.data.VitalEvent
import com.example.meditrack.data.VitalType
import com.example.meditrack.graph.GraphPoint
import com.example.meditrack.ui.VitalChartView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalDetailScreen(navController: NavController, type: VitalType) {

    val vm = vitalViewModel()
    val continuous = vm.isContinuous(type)

    val reading = if (continuous)
        remember(type) { vm.liveStream(type) }.collectAsState(initial = null).value
    else null
    val latest = remember(type) { vm.latest(type) }.collectAsState(initial = null).value
    val count by vm.historyCount.collectAsState(initial = 0)

    val v1 = reading?.value1 ?: latest?.val1
    val v2 = reading?.value2 ?: latest?.val2?.takeIf { type.hasSecondValue && it != 0.0 }
    val status = v1?.let { vm.classify(type, it, v2) }

    val livePoints: List<GraphPoint> = if (continuous)
        remember(type) { vm.liveGraph(type) }.collectAsState().value
    else emptyList()

    var days by remember { mutableStateOf(1) }
    val history by remember(type, days) { vm.historyGraph(type, days) }
        .collectAsState(initial = emptyList())

    val events = remember { vm.events() }.collectAsState(initial = emptyList()).value
        .filter { it.type == type }

    var showLog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(type.displayName, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── Current value + status ─────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                        Text(
                            if (continuous) "Live" else "Latest logged",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                if (v1 != null) formatVital(type, v1, v2) else "No data yet",
                                style = MaterialTheme.typography.displaySmall
                            )
                            if (v1 != null) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    type.defaultUnit,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                        if (status != null) {
                            Spacer(Modifier.height(10.dp))
                            StatusChip(status.name, statusColor(status))
                        }
                    }
                }
            }

            // ── Live chart ─────────────────────────────────────────────────────
            if (continuous) {
                item {
                    SectionTitle("Live monitor")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        AndroidView(
                            factory = { ctx -> VitalChartView(ctx) },
                            update = { it.setData(type, livePoints, follow = true) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .padding(8.dp)
                        )
                    }
                }
            }

            // ── History chart + range toggle ───────────────────────────────────
            item {
                SectionTitle("History")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RangeChip("1D", days == 1) { days = 1 }
                    RangeChip("1W", days == 7) { days = 7 }
                    RangeChip("1M", days == 30) { days = 30 }
                }
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    AndroidView(
                        factory = { ctx -> VitalChartView(ctx) },
                        update = { it.setData(type, history, follow = false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .padding(8.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "$count readings recorded in total",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // ── Manual log button ──────────────────────────────────────────────
            item {
                Button(
                    onClick = { showLog = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Log a reading manually", style = MaterialTheme.typography.labelMedium) }
            }

            // ── Alerts / events ────────────────────────────────────────────────
            item { SectionTitle("Alerts") }
            if (events.isEmpty()) {
                item {
                    Text(
                        "No abnormal episodes recorded.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(events) { event -> EventRow(type, event) }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showLog) {
        LogVitalDialog(
            type = type,
            onDismiss = { showLog = false },
            onLog = { a, b, note ->
                vm.logVital(type, a, b, note)
                showLog = false
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) }
    )
}

@Composable
private fun EventRow(type: VitalType, event: VitalEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(statusColor(event.status), CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(event.status.name, style = MaterialTheme.typography.labelMedium, color = statusColor(event.status))
                Spacer(Modifier.weight(1f))
                Text("${event.durationMillis / 1000}s", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Peak ${formatVital(type, event.extremeValue, event.extremeValue2)} ${type.defaultUnit}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${event.startTimestamp.toTime()} → ${event.endTimestamp.toTime()}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/** Manual entry dialog — two fields for blood pressure, one for everything else. */
@Composable
private fun LogVitalDialog(
    type: VitalType,
    onDismiss: () -> Unit,
    onLog: (Double, Double?, String?) -> Unit,
) {
    var val1 by remember { mutableStateOf("") }
    var val2 by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val a = val1.toDoubleOrNull() ?: return@Button
                    val b = if (type.hasSecondValue) (val2.toDoubleOrNull() ?: return@Button) else null
                    onLog(a, b, note.ifBlank { null })
                }
            ) { Text("Log", style = MaterialTheme.typography.labelSmall) }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel", style = MaterialTheme.typography.labelSmall) }
        },
        title = { Text("Log ${type.displayName}", style = MaterialTheme.typography.bodyLarge) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(
                    value = val1,
                    onValueChange = { v -> if (v.all { it.isDigit() || it == '.' }) val1 = v },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    label = { Text(if (type.hasSecondValue) "Systolic" else "Value") },
                    suffix = { Text(type.defaultUnit, style = MaterialTheme.typography.labelSmall) }
                )
                if (type.hasSecondValue) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = val2,
                        onValueChange = { v -> if (v.all { it.isDigit() || it == '.' }) val2 = v },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        label = { Text("Diastolic") },
                        suffix = { Text(type.defaultUnit, style = MaterialTheme.typography.labelSmall) }
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    singleLine = true,
                    label = { Text("Note (optional)") }
                )
            }
        }
    )
}
