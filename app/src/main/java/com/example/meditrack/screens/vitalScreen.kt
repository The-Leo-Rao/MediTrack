package com.example.meditrack.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bloodtype
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.meditrack.Notification.NotificationHelper
import com.example.meditrack.data.VitalType
import com.example.meditrack.graph.GraphPoint
import com.example.meditrack.clinical.VitalStatus
import com.example.meditrack.report.ReportShare
import com.example.meditrack.report.SOSDispatcher
import com.example.meditrack.report.VitalsReportGenerator
import com.example.meditrack.ui.VitalViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/** The order vitals are shown in — fastest-moving (most "alive") first. */
private val vitalOrder = listOf(
    VitalType.HEART_RATE,
    VitalType.SPO2,
    VitalType.BLOOD_PRESSURE,
    VitalType.TEMPERATURE,
    VitalType.BLOOD_SUGAR,
    VitalType.WEIGHT,
)

@Composable
fun VitalScreen(navController: NavController) {

    val context = LocalContext.current
    val vm = vitalViewModel()
    val isMonitoring by vm.isMonitoringFlow.collectAsState()

    val readingCount by vm.historyCount.collectAsState(initial = 0)

    var pressed by remember { mutableStateOf(false) }

    var maybeSOS by remember { mutableStateOf(false) }
    var SOScalled by remember { mutableStateOf(false) }
    var sendingSOS by remember { mutableStateOf(false) }
    var clickCount by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    var reportFile by remember { mutableStateOf<File?>(null) }
    var exportError by remember { mutableStateOf<String?>(null) }

    fun startExport() {
        if (exporting) return
        exporting = true
        scope.launch {
            val outcome = runCatching { VitalsReportGenerator.generate(context) }
            exporting = false
            outcome
                .onSuccess { reportFile = it }
                .onFailure { exportError = it.message ?: "Could not generate the report." }
        }
    }


    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { VitalsBottomBar(navController) }
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(8.dp))
                        Text("VITALS", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        if(isMonitoring){
                        Text(
                            "Sensor Live · $readingCount readings recorded",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        }else{
                        Text(
                            "Sensor Offline · $readingCount readings recorded",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                items(vitalOrder) { type ->
                    VitalCard(vm, type) {
                        navController.navigate("vitalDetail/${type.name}")
                    }
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable(enabled = !exporting) { startExport() },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (exporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Generating report…", style = MaterialTheme.typography.bodyMedium)
                            } else {
                                Text("Export to PDF", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.width(10.dp))
                                Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(60.dp)
                    .clickable{maybeSOS=true}
                    .background(Color(90, 30, 30), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("SOS") }


        }
    }

    if(maybeSOS){
        AlertDialog(
            onDismissRequest = { maybeSOS=false },
            confirmButton = {
                Button(
                    onClick = {
                        clickCount++

                        if (clickCount == 5) {
                            maybeSOS=false
                            sendingSOS = true
                            if (
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                val notificationHelper= NotificationHelper(context)
                                scope.launch {
                                    SOSDispatcher.dispatch(
                                        context = context,
                                        onDone  = { success, msg ->
                                            SOScalled = true
                                            sendingSOS=false
                                            clickCount = 0
                                            maybeSOS=false
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Text("Call SOS")
                }
            },
            text={
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Click the button 5 times to call SOS", textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)

                    Spacer(Modifier.height(15.dp))

                    Text("Click ${5-clickCount} more times to call SOS")
                }
            }
        )
    }

    if (sendingSOS) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            dismissButton = {},
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()

                    Spacer(Modifier.height(20.dp))

                    Text(
                        "Calling SOS...\nGetting location and preparing medical report.",
                        textAlign = TextAlign.Center
                    )
                }
            }
        )
    }

    if (SOScalled) {
        AlertDialog(
            onDismissRequest = { SOScalled = false },
            confirmButton = {},
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "SOS Has been called and your emergency contact has been notified",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        )
    }

    reportFile?.let { file ->
        AlertDialog(
            onDismissRequest = { reportFile = null },
            confirmButton = {
                Button(onClick = {
                    ReportShare.open(context, file)
                    reportFile = null
                }) { Text("Open", style = MaterialTheme.typography.labelSmall) }
            },
            dismissButton = {
                Button(onClick = {
                    ReportShare.share(context, file)
                    reportFile = null
                }) { Text("Share", style = MaterialTheme.typography.labelSmall) }
            },
            title = { Text("Report ready", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "Your health & vitals report has been generated.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        )
    }

    exportError?.let { msg ->
        AlertDialog(
            onDismissRequest = { exportError = null },
            confirmButton = {
                Button(onClick = { exportError = null }) {
                    Text("OK", style = MaterialTheme.typography.labelSmall)
                }
            },
            title = { Text("Export failed", style = MaterialTheme.typography.titleLarge) },
            text = { Text(msg, style = MaterialTheme.typography.bodyMedium) }
        )
    }
}

/** One vital tile: name + icon + live value + status + a live sparkline. */
@Composable
private fun VitalCard(vm: VitalViewModel, type: VitalType, onClick: () -> Unit) {
    val isMonitoring by vm.isMonitoringFlow.collectAsState()
    val continuous = type.continuous

    val reading = if (vm.isContinuous(type) && isMonitoring)
        remember(type) { vm.liveStream(type) }.collectAsState(initial = null).value
    else
        null

    val points: List<GraphPoint> = if (continuous)
        remember(type) { vm.liveGraph(type) }.collectAsState().value
    else emptyList()

    val latest = remember(type) { vm.latest(type) }.collectAsState(initial = null).value

    val v1 = reading?.value1 ?: latest?.val1
    val v2 = reading?.value2 ?: latest?.val2?.takeIf { type.hasSecondValue && it != 0.0 }
    val status = v1?.let { vm.classify(type, it, v2) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    iconFor(type),
                    contentDescription = type.displayName,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    type.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (v1 != null) formatVital(type, v1, v2) else "—",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    type.defaultUnit,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            if (continuous && points.size >= 2) {
                Sparkline(
                    points = points,
                    color = statusColor(status ?: VitalStatus.NORMAL),
                    modifier = Modifier.fillMaxWidth().height(34.dp)
                )
            } else if (!continuous) {
                Text("Tap to log", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** A minimal line chart of the live buffer — pretty, cheap, no chart library. */
@Composable
internal fun Sparkline(points: List<GraphPoint>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val values = points.map { it.value }
        val min = values.min()
        val max = values.max()
        val range = (max - min).coerceAtLeast(0.0001)
        val stepX = size.width / (points.size - 1)
        val path = Path()
        points.forEachIndexed { i, p ->
            val x = i * stepX
            val y = (size.height - ((p.value - min) / range * size.height)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = Stroke(width = 3f))
    }
}

/** Activity-scoped ViewModel so monitoring keeps running across navigation. */
@Composable
internal fun vitalViewModel(): VitalViewModel {
    val owner = LocalContext.current as ComponentActivity
    return viewModel(viewModelStoreOwner = owner)
}

internal fun iconFor(type: VitalType): ImageVector = when (type) {
    VitalType.HEART_RATE -> Icons.Rounded.MonitorHeart
    VitalType.SPO2 -> Icons.Rounded.Air
    VitalType.BLOOD_PRESSURE -> Icons.Rounded.Bloodtype
    VitalType.TEMPERATURE -> Icons.Rounded.Thermostat
    VitalType.BLOOD_SUGAR -> Icons.Rounded.WaterDrop
    VitalType.WEIGHT -> Icons.Rounded.MonitorWeight
}

internal fun statusColor(status: VitalStatus): Color = when (status) {
    VitalStatus.NORMAL -> Color(0xFF43A047)
    VitalStatus.WARNING -> Color(0xFFFB8C00)
    VitalStatus.CRITICAL -> Color(0xFFD32F2F)
}

/** Format a reading for display (BP as systolic/diastolic, temperature with a decimal). */
internal fun formatVital(type: VitalType, v1: Double, v2: Double?): String = when {
    type.hasSecondValue && v2 != null -> "${v1.toInt()}/${v2.toInt()}"
    type == VitalType.TEMPERATURE -> String.format("%.1f", v1)
    else -> v1.toInt().toString()
}

/** The shared bottom navigation bar used across the main screens. */
@Composable
internal fun VitalsBottomBar(navController: NavController) {
    NavigationBar {
        val currentRoute =
            navController.currentBackStackEntryAsState().value?.destination?.route
        NavigationBarItem(
            selected = currentRoute == "vitals",
            onClick = { navController.navigate("vitals") },
            icon = {
                Icon(
                    Icons.Rounded.AcUnit,
                    contentDescription = "Vitals",
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            label = {
                Text(
                    "Vitals",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        )
        NavigationBarItem(
            selected = currentRoute == "doctors",
            onClick = { navController.navigate("doctors") },
            icon = {
                Icon(
                    Icons.Rounded.MedicalServices,
                    contentDescription = "Records",
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            label = {
                Text(
                    "Records",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        )
        NavigationBarItem(
            selected = currentRoute == "reminders",
            onClick = { navController.navigate("reminders") },
            icon = {
                Icon(
                    Icons.Rounded.Notifications,
                    contentDescription = "reminders",
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            label = {
                Text(
                    "Reminders",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        )
        NavigationBarItem(
            selected = currentRoute == "profile",
            onClick = { navController.navigate("profile") },
            icon = {
                Icon(
                    Icons.Rounded.AccountCircle,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            label = {
                Text(
                    "Profile",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        )
    }
}
