package com.example.meditrack.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Button
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.meditrack.DBHelper
import com.example.meditrack.Notification.NotificationHelper
import kotlinx.coroutines.delay
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.meditrack.Vital
import com.example.meditrack.data.VitalType
import com.example.meditrack.graph.GraphPoint
import com.example.meditrack.ui.VitalChartView
import java.util.Calendar

@Composable
fun VitalScreen(navController: NavController){

    val context = LocalContext.current
    val dbHelper= DBHelper(context)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar ={
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
                            style = MaterialTheme.typography.labelSmall) }
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
                    selected = currentRoute=="reminders",
                    onClick ={navController.navigate("reminders")},
                    icon={
                        Icon(
                            Icons.Rounded.Notifications,
                            contentDescription="reminders",
                            tint = MaterialTheme.colorScheme.secondary)},
                    label={
                        Text(
                            "Reminders",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelSmall
                        )}
                )
                NavigationBarItem(
                    selected = currentRoute=="profile",
                    onClick ={navController.navigate("profile")},
                    icon={
                        Icon(
                            Icons.Rounded.AccountCircle,
                            contentDescription="Profile",
                            tint = MaterialTheme.colorScheme.secondary)},
                    label={
                        Text(
                            "Profile",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelSmall
                        )}
                )
            }
        }
    ){padding->

        var pressed by remember { mutableStateOf(false) }
        var SOScalled by remember { mutableStateOf(false) }

        var dialog by remember { mutableStateOf(false) }
        var type by remember { mutableStateOf("") }

        var BP by remember { mutableStateOf(false) }
        var sp02 by remember { mutableStateOf(false) }
        var temp by remember { mutableStateOf(false) }
        var weight by remember { mutableStateOf(false) }
        var hRate by remember { mutableStateOf(false) }
        var sugar by remember { mutableStateOf(false) }
        var monthly by remember { mutableStateOf(false) }

        LaunchedEffect(pressed) {
            if (pressed) {
                delay(3000)
                if (
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val notificationHelper = NotificationHelper(context)
                    notificationHelper.CallSOS()
                }
                SOScalled = true
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 20.dp)
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
                    .background(Color(90,30,30), shape = CircleShape),
                contentAlignment = Alignment.Center
            ){Text("SOS")}

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item { Spacer(Modifier.height(15.dp)) }

                item { Text("VITALS", style = MaterialTheme.typography.titleLarge) }

                item { Spacer(Modifier.height(30.dp)) }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(15.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(175.dp)
                                .clickable{
                                    type="BLOOD PRESSURE"
                                    dialog=true
                                          },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(15.dp)
                            ) {
                                Text(
                                    "Blood Pressure",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.align(Alignment.TopStart)
                                )
                            }
                        }
                        Spacer(Modifier.padding(15.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(175.dp)
                                .clickable{
                                    type="SPO2"
                                    dialog=true},
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(15.dp)
                            ) {
                                Text(
                                    "SpO2",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.align(Alignment.TopStart)
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(15.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clickable{
                                    type="BODY TEMPERATURE"
                                    dialog=true},
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(15.dp)
                            ) {
                                Text(
                                    "Body Temperature",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.align(Alignment.TopStart)
                                )
                            }
                        }
                        Spacer(Modifier.width(15.dp))
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clickable{
                                    type="WEIGHT"
                                    dialog=true},
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(15.dp)
                            ) {
                                Text(
                                    "Weight",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.align(Alignment.TopStart)
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(15.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clickable{
                                    type="HEART RATE"
                                    dialog=true},
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(15.dp)
                            ) {
                                Text(
                                    "Heart rate",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.align(Alignment.TopStart)
                                )
                            }
                        }
                        Spacer(Modifier.width(15.dp))
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clickable{
                                    type="BLOOD SUGAR"
                                    dialog=true},
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(15.dp)
                            ) {
                                Text(
                                    "Blood Sugar",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.align(Alignment.TopStart)
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .padding(15.dp)
                            .fillMaxWidth()
                            .height(60.dp)
                            .clickable{},
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Export to PDF",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(Modifier.width(10.dp))

                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                        }
                    }
                }

                item { Spacer(Modifier.height(30.dp)) }
            }
        }

        if(dialog){
            Dialog(
                onDismissRequest = {dialog=false}
            ){

                val data = dbHelper.getAVital(type)

                val monthStart = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val weekStart = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.MONDAY
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val filtered = if (monthly) {
                    data.filter { it.timestamp >= monthStart }
                } else {
                    data.filter { it.timestamp >= weekStart }
                }

                val points = filtered
                    .groupBy { vital ->

                        val cal = Calendar.getInstance().apply {
                            timeInMillis = vital.timestamp

                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        cal.timeInMillis
                    }
                    .map { (dayStart, records) ->
                        GraphPoint(
                            t = dayStart,
                            value = records.map { it.val1 }.average(),
                            value2 = records.map { it.val2 }.average()
                        )
                    }
                    .sortedBy { it.t }


                val vitalType = when (type) {
                    "HEART RATE" -> VitalType.HEART_RATE
                    "BLOOD PRESSURE" -> VitalType.BLOOD_PRESSURE
                    "SPO2" -> VitalType.SPO2
                    "BODY TEMPERATURE" -> VitalType.TEMPERATURE
                    "BLOOD SUGAR"-> VitalType.BLOOD_SUGAR
                    else -> VitalType.HEART_RATE
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors= CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ){
                    Column(
                        modifier = Modifier
                            .padding(vertical = 15.dp, horizontal = 15.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ){

                        Row(verticalAlignment = Alignment.CenterVertically){
                            Text("Weekly", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.width(5.dp))
                            Switch(
                                checked = monthly,
                                onCheckedChange = { monthly = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.primary,
                                )
                            )
                            Spacer(Modifier.width(5.dp))
                            Text("Monthly", style = MaterialTheme.typography.bodyMedium)
                        }

                        Spacer(Modifier.height(10.dp))

                        AndroidView(
                            factory = { context ->
                                VitalChartView(context)
                            },
                            update = { chart ->
                                chart.setData(vitalType, points, follow = false)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        )

                        Spacer(Modifier.height(25.dp))

                        Text("pinch to zoom, scroll",style= MaterialTheme.typography.labelSmall)

                        Spacer(Modifier.height(25.dp))

                        Button(onClick = {
                            when{
                                type.equals("HEART RATE")->{hRate=true}
                                type.equals("BLOOD SUGAR")->{sugar=true}
                                type.equals("WEIGHT")->{weight=true}
                                type.equals("BODY TEMPERATURE")->{temp=true}
                                type.equals("BLOOD PRESSURE")->{BP=true}
                                type.equals("SPO2")->{sp02=true}
                            }
                        }){Text("Add Record", style = MaterialTheme.typography.labelSmall)}
                    }
                }
            }
        }


        if(SOScalled){
            AlertDialog(
                onDismissRequest = {},
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

        if(hRate){

            var val1 by remember { mutableStateOf("") }
            var note by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = {hRate=false},
                confirmButton = {
                    Button(
                        onClick = {
                            when{
                                val1.isBlank()->{}
                                else->{
                                    dbHelper.setAVital("HEART RATE",val1.toDouble(),0.0,"bpm",System.currentTimeMillis(),note)
                                    hRate=false
                                }
                            }
                        }
                    ){Text("Log", style = MaterialTheme.typography.labelSmall)}
                },
                title = {Text("Log an entry", style = MaterialTheme.typography.bodyLarge)},
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = val1,
                            onValueChange = { newValue ->
                                if (newValue.all { it.isDigit() || it == '.' }) {
                                    val1 = newValue
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            singleLine = true,
                            label={Text("Enter heart rate")},
                            suffix = {Text("bpm", style = MaterialTheme.typography.labelSmall)}
                        )

                        Spacer(Modifier.height(15.dp))

                        OutlinedTextField(
                            value = note,
                            onValueChange = {note=it},
                            singleLine = true,
                            label={Text("Enter note if any")}
                        )
                    }
                }
            )
        }

        if(sugar){

            var val1 by remember { mutableStateOf("") }
            var note by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = {sugar=false},
                confirmButton = {
                    Button(
                        onClick = {
                            when{
                                val1.isBlank()->{}
                                else->{
                                    dbHelper.setAVital("BLOOD SUGAR",val1.toDouble(),0.0,"mg/dL",System.currentTimeMillis(),note)
                                    sugar=false
                                }
                            }
                        }
                    ){Text("Log", style = MaterialTheme.typography.labelSmall)}
                },
                title = {Text("Log an entry", style = MaterialTheme.typography.bodyLarge)},
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = val1,
                            onValueChange = { newValue ->
                                if (newValue.all { it.isDigit() || it == '.' }) {
                                    val1 = newValue
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            singleLine = true,
                            label={Text("Enter blood sugar reading")},
                            suffix = {Text("mg/dL", style = MaterialTheme.typography.labelSmall)}
                        )

                        Spacer(Modifier.height(15.dp))

                        OutlinedTextField(
                            value = note,
                            onValueChange = {note=it},
                            singleLine = true,
                            label={Text("Enter note if any")}
                        )
                    }
                }
            )
        }

        if(temp){

            var val1 by remember { mutableStateOf("") }
            var note by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = {temp=false},
                confirmButton = {
                    Button(
                        onClick = {
                            when{
                                val1.isBlank()->{}
                                else->{
                                    dbHelper.setAVital("BODY TEMPERATURE",val1.toDouble(),0.0,"°C",System.currentTimeMillis(),note)
                                    temp=false
                                }
                            }
                        }
                    ){Text("Log", style = MaterialTheme.typography.labelSmall)}
                },
                title = {Text("Log an entry", style = MaterialTheme.typography.bodyLarge)},
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = val1,
                            onValueChange = { newValue ->
                                if (newValue.all { it.isDigit() || it == '.' }) {
                                    val1 = newValue
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            singleLine = true,
                            label={Text("Enter body temperature")},
                            suffix = {Text("°C", style = MaterialTheme.typography.labelSmall)}
                        )

                        Spacer(Modifier.height(15.dp))

                        OutlinedTextField(
                            value = note,
                            onValueChange = {note=it},
                            singleLine = true,
                            label={Text("Enter note if any")}
                        )
                    }
                }
            )
        }

        if(weight){

            var val1 by remember { mutableStateOf("") }
            var note by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = {weight=false},
                confirmButton = {
                    Button(
                        onClick = {
                            when{
                                val1.isBlank()->{}
                                else->{
                                    dbHelper.setAVital("WEIGHT",val1.toDouble(),0.0,"kg",System.currentTimeMillis(),note)
                                    weight=false
                                }
                            }
                        }
                    ){Text("Log", style = MaterialTheme.typography.labelSmall)}
                },
                title = {Text("Log an entry", style = MaterialTheme.typography.bodyLarge)},
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = val1,
                            onValueChange = { newValue ->
                                if (newValue.all { it.isDigit() || it == '.' }) {
                                    val1 = newValue
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            singleLine = true,
                            label={Text("Enter body weight")},
                            suffix = {Text("kg", style = MaterialTheme.typography.labelSmall)}
                        )

                        Spacer(Modifier.height(15.dp))

                        OutlinedTextField(
                            value = note,
                            onValueChange = {note=it},
                            singleLine = true,
                            label={Text("Enter note if any")}
                        )
                    }
                }
            )
        }

        if(BP){

            var val1 by remember { mutableStateOf("") }
            var val2 by remember { mutableStateOf("") }
            var note by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = {BP=false},
                confirmButton = {
                    Button(
                        onClick = {
                            when{
                                val1.isBlank()->{}
                                val2.isBlank()->{}
                                else->{
                                    dbHelper.setAVital("BLOOD PRESSURE",val1.toDouble(),val2.toDouble(),"mmHg",System.currentTimeMillis(),note)
                                    BP=false
                                }
                            }
                        }
                    ){Text("Log", style = MaterialTheme.typography.labelSmall)}
                },
                title = {Text("Log an entry", style = MaterialTheme.typography.bodyLarge)},
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = val1,
                            onValueChange = { newValue ->
                                if (newValue.all { it.isDigit() || it == '.' }) {
                                    val1 = newValue
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            singleLine = true,
                            label={Text("Enter systolic pressure")},
                            suffix = {Text("mmHg", style = MaterialTheme.typography.labelSmall)}
                        )

                        Spacer(Modifier.height(15.dp))

                        OutlinedTextField(
                            value = val2,
                            onValueChange = { newValue ->
                                if (newValue.all { it.isDigit() || it == '.' }) {
                                    val2 = newValue
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            singleLine = true,
                            label={Text("Enter diastolic pressure")},
                            suffix = {Text("mmHg", style = MaterialTheme.typography.labelSmall)}
                        )

                        Spacer(Modifier.height(15.dp))

                        OutlinedTextField(
                            value = note,
                            onValueChange = {note=it},
                            singleLine = true,
                            label={Text("Enter note if any")}
                        )
                    }
                }
            )
        }

        if(sp02){

            var val1 by remember { mutableStateOf("") }
            var note by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = {sp02=false},
                confirmButton = {
                    Button(
                        onClick = {
                            when{
                                val1.isBlank()->{}
                                else->{
                                    dbHelper.setAVital("SPO2",val1.toDouble(),0.0,"%",System.currentTimeMillis(),note)
                                    sp02=false
                                }
                            }
                        }
                    ){Text("Log", style = MaterialTheme.typography.labelSmall)}
                },
                title = {Text("Log an entry", style = MaterialTheme.typography.bodyLarge)},
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = val1,
                            onValueChange = { newValue ->
                                if (newValue.all { it.isDigit() || it == '.' }) {
                                    val1 = newValue
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            singleLine = true,
                            label={Text("Enter SpO2")},
                            suffix = {Text("%", style = MaterialTheme.typography.labelSmall)}
                        )

                        Spacer(Modifier.height(15.dp))

                        OutlinedTextField(
                            value = note,
                            onValueChange = {note=it},
                            singleLine = true,
                            label={Text("Enter note if any")}
                        )
                    }
                }
            )
        }
    }
}