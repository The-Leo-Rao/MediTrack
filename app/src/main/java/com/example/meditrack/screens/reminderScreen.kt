package com.example.meditrack.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.meditrack.DBHelper
import com.example.meditrack.Notification.NotificationHelper
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun reminderScreen(navController: NavController){

    val context = LocalContext.current
    val dbHelper = DBHelper(context)

    var showDialog by remember { mutableStateOf(false) }
    var multi by remember { mutableStateOf(false) }
    var first by remember { mutableStateOf(false) }
    var second by remember { mutableStateOf(false) }
    var delRem by remember { mutableStateOf(false) }
    var SOScalled by remember { mutableStateOf(false) }

    var presc by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var time1: Long by remember { mutableStateOf(0) }
    var time2: Long by remember { mutableStateOf(0) }
    var idToDel by remember { mutableStateOf(0) }

    var prescLabel by remember { mutableStateOf("Prescription") }
    var doseLabel by remember { mutableStateOf("Enter dosage") }

    var pressed by remember { mutableStateOf(false) }
    LaunchedEffect(pressed) {
        if (pressed) {
            delay(3000)
            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val notificationHelper= NotificationHelper(context)
                notificationHelper.CallSOS()
                SOScalled=true
            }
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar ={
            Column {
                Spacer(Modifier.height(15.dp))
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

        }
    ){padding->

        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)){
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val reminders = dbHelper.getAllRem()

                item { Spacer(Modifier.height(15.dp)) }

                item { Text("REMINDERS", style = MaterialTheme.typography.titleLarge) }

                item { Spacer(Modifier.height(50.dp)) }

                items(reminders) { reminder ->
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 5.dp)
                            .fillMaxWidth()
                            .clickable {
                                delRem = true
                                idToDel = reminder.id
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(5.dp)) {
                            Text(
                                "Reminder for prescription ${reminder.med} of dosage ${reminder.dose}",
                                modifier = Modifier.align(Alignment.Start)
                            )

                            Spacer(Modifier.height(15.dp))

                            Text(
                                "At- " + reminder.time1.toTime().substring(7)
                            )
                            if (reminder.time2.toInt() != 0) {
                                Text("and " + reminder.time2.toTime().substring(7))
                            }
                        }
                    }

                    Spacer(Modifier.height(15.dp))
                }
            }

            Button(
                onClick = { showDialog = true },
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(70.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Icon(
                    Icons.Rounded.AddCircleOutline,
                    contentDescription = "Add Reminder",
                    modifier = Modifier.size(40.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 20.dp)
                    .size(60.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                pressed = true
                                try {
                                    tryAwaitRelease()
                                } finally {
                                    pressed = false
                                }
                            }
                        )
                    }
                    .background(Color(90, 30, 30), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("SOS") }

        }
    }

    if(showDialog){
        AlertDialog(
            onDismissRequest = {showDialog=false},
            confirmButton = {
                Button(
                    onClick = {
                        when{
                            presc.isBlank()->{prescLabel="Prescription cannot be Empty" }
                            dose.isBlank()->{doseLabel="Dosage cannot be empty"}

                            else->{
                                dbHelper.AddReminder(presc, dose,time1,time2)
                                presc = ""
                                dose = ""
                                time1 = 0L
                                time2 = 0L
                                multi = false
                                prescLabel = "Prescription"
                                doseLabel = "Enter dosage"
                                showDialog=false
                            }
                        }
                    }
                ) {Text("Confirm", style = MaterialTheme.typography.labelSmall)}
            },
            dismissButton = {},
            title = {Text("Add new reminder", style = MaterialTheme.typography.bodyLarge)},
            text = {
                Column(
                    modifier = Modifier
                        .padding(10.dp)
                ) {
                    OutlinedTextField(
                        value = presc,
                        onValueChange = { presc = it },
                        label = {
                            Text(
                                prescLabel,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )

                    OutlinedTextField(
                        value = dose,
                        onValueChange = { dose = it },
                        label = {
                            Text(
                                doseLabel,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )

                    Spacer(Modifier.height(15.dp))

                    Row(verticalAlignment = Alignment.CenterVertically){
                        Text("Two a day", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(5.dp))
                        Switch(checked = multi, onCheckedChange = { multi = it })
                    }

                    Spacer(Modifier.height(15.dp))

                    Row(verticalAlignment = Alignment.CenterVertically){
                        Text("Alarm time", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { first = true }) {
                            Icon(
                                Icons.Rounded.AccessTime,
                                contentDescription = null
                            )
                        }
                    }

                    Spacer(Modifier.height(15.dp))

                    if(multi){
                        Row(verticalAlignment = Alignment.CenterVertically){
                            Text("Second alarm time", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.weight(1f))
                            Button(onClick = { second = true }) {
                                Icon(
                                    Icons.Rounded.AccessTime,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    if(first){
        val timePickerState = rememberTimePickerState(
            initialHour = 12,
            initialMinute = 0,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { first = false },
            confirmButton = {
                Button(
                    onClick = {
                        val calendar = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        time1 = calendar.timeInMillis
                        first = false
                    }
                ) {
                    Text("OK",style= MaterialTheme.typography.labelSmall)
                }
            },
            dismissButton = {
                Button(
                    onClick = { first = false }
                ) {
                    Text("Cancel", style = MaterialTheme.typography.labelSmall)
                }
            },
            text = {
                TimePicker(
                    state = timePickerState
                )
            }
        )
    }

    if(second){
        val timePickerState = rememberTimePickerState(
            initialHour = 12,
            initialMinute = 0,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { second = false },
            confirmButton = {
                Button(
                    onClick = {
                        val calendar = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        time2 = calendar.timeInMillis
                        second = false
                    }
                ) {
                    Text("OK",style= MaterialTheme.typography.labelSmall)
                }
            },
            dismissButton = {
                Button(
                    onClick = { second = false }
                ) {
                    Text("Cancel", style = MaterialTheme.typography.labelSmall)
                }
            },
            text = {
                TimePicker(
                    state = timePickerState
                )
            }
        )
    }

    if(delRem){
        AlertDialog(
            onDismissRequest = {delRem=false},
            dismissButton = {
                Button(onClick = {delRem=false}){Text("Cancel",style = MaterialTheme.typography.labelSmall)}
            },
            confirmButton = {Button(onClick ={
                var deleted=dbHelper.delRem(idToDel)
                if(deleted!=0){
                    delRem=false
                }
            }){Text("Confirm",style = MaterialTheme.typography.labelSmall)}},
            title = {Text("Delete Record?",style = MaterialTheme.typography.bodyMedium)}
        )
    }

    if(SOScalled){
        AlertDialog(
            onDismissRequest = {SOScalled=false},
            confirmButton = {},
            text={
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("SOS Has been called and your emergency contact has been notified", textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
                }
            }
        )
    }
}
