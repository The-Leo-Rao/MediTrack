package com.example.meditrack.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold

import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.meditrack.DBHelper
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.meditrack.NotificationHelper
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocScreen(navController: NavController){

    val context = LocalContext.current
    NotificationHelper(context).createChannel()

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val locationGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

            val smsGranted =
                permissions[Manifest.permission.SEND_SMS] ?: false

            val notificationGranted =
                permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false

            Log.d("PERMS", "Location: $locationGranted")
            Log.d("PERMS", "SMS: $smsGranted")
            Log.d("PERMS", "Notifications: $notificationGranted")
        }

    LaunchedEffect(Unit) {

        val locationGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val smsGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED

        if (!locationGranted || !smsGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            )
        }
    }

    val dbHelper = DBHelper(context)
    val notificationHelper = NotificationHelper(context)

    var delAlert by remember { mutableStateOf(false) }
    var SOScalled by remember { mutableStateOf(false) }
    var idToDel: Int by remember { mutableStateOf(0) }

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
                Log.d("SOS", "Permission granted")
                notificationHelper.CallSOS()
                SOScalled=true
            }
            else{Log.d("SOS","Permission denied") }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar ={
            Column{

                var type by remember { mutableStateOf("") }
                var content by remember { mutableStateOf("") }
                var showDialog by remember{ mutableStateOf(false) }

                var newPrescription by remember { mutableStateOf(false) }
                var newSymptom by remember { mutableStateOf(false) }
                var newNote by remember { mutableStateOf(false) }
                var followup by remember { mutableStateOf(false) }
                var imageupload  by remember { mutableStateOf(false) }

                Spacer(Modifier.height(5.dp))

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    onClick = {showDialog=true},
                    shape = RoundedCornerShape(15.dp)
                ){
                    Icon(Icons.Rounded.AddCircleOutline, contentDescription = "New Entry")
                }

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

                if(showDialog){
                    AlertDialog(
                        onDismissRequest = {showDialog=false},
                        confirmButton = {},
                        title = {Text("Choose an entry to add", style = MaterialTheme.typography.titleLarge)},
                        text={
                            Column(
                                modifier = Modifier
                                    .padding(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        type="CONSULTATION REPORT"
                                        imageupload=true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp),
                                    shape = RoundedCornerShape(15.dp)
                                ){ Text("Consultation Report image",style = MaterialTheme.typography.bodyMedium)}

                                Spacer(Modifier.height(10.dp))

                                Button(
                                    onClick={
                                        type="DOCTORS NOTE"
                                        newNote=true
                                    },
                                    modifier= Modifier
                                        .fillMaxWidth()
                                        .height(40.dp),
                                    shape = RoundedCornerShape(15.dp)
                                ) {Text("Doctors note",style = MaterialTheme.typography.bodyMedium)}

                                Spacer(Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        type="NEW PRESCRIPTION"
                                        newPrescription=true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp),
                                    shape = RoundedCornerShape(15.dp)
                                ){Text("Add a prescription",style = MaterialTheme.typography.bodyMedium)}

                                Spacer(Modifier.height(10.dp))

                                Button(
                                    onClick={
                                        type="NEW SYMPTOM"
                                        newSymptom=true
                                    },
                                    modifier= Modifier
                                        .fillMaxWidth()
                                        .height(40.dp),
                                    shape = RoundedCornerShape(15.dp)
                                ) {Text("Log new symptom",style = MaterialTheme.typography.bodyMedium)}

                                Spacer(Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        type="FOLLOW UP"
                                        followup=true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp),
                                    shape = RoundedCornerShape(15.dp)
                                ){ Text("Schedule follow-up",style = MaterialTheme.typography.bodyMedium)}
                            }
                        }

                    )
                }

                if(newNote){
                    Dialog(onDismissRequest = {newNote=false}){
                        Card(colors= CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)){
                            Column(
                                modifier = Modifier
                                    .padding(vertical = 15.dp, horizontal = 15.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ){
                                var note by remember { mutableStateOf("") }

                                Text(
                                    "Enter Doctors Note",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.align(Alignment.Start))



                                Spacer(Modifier.height(15.dp))

                                OutlinedTextField(
                                    value = note,
                                    onValueChange = {note=it},
                                    singleLine = false,
                                    label={Text("Enter Note")}
                                )

                                Spacer(Modifier.height(15.dp))

                                Button(
                                    onClick = {
                                        dbHelper.AddRecord(type,note, System.currentTimeMillis())
                                        showDialog=false
                                        newNote=false
                                    },
                                    modifier = Modifier.align(Alignment.End)){Text("Log",style = MaterialTheme.typography.labelSmall)}

                            }
                        }
                    }
                }

                if(followup){
                    val state= rememberDatePickerState()
                    DatePickerDialog(
                        onDismissRequest = {followup=false},
                        confirmButton = {
                            Button(
                                onClick = {
                                    val date=state.selectedDateMillis.toString()
                                    dbHelper.AddRecord(type,date, System.currentTimeMillis())
                                    showDialog=false
                                    followup=false
                                }){Text("Log",style = MaterialTheme.typography.labelSmall)}
                        }
                    ){DatePicker(state=state)}
                }

                if(newSymptom){
                    Dialog(onDismissRequest = {newSymptom=false}){
                        Card(colors= CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)){
                            Column(
                                modifier = Modifier
                                    .padding(vertical = 15.dp, horizontal = 15.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ){
                                var note by remember { mutableStateOf("") }

                                Text(
                                    "Enter new symptom",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.align(Alignment.Start))


                                Spacer(Modifier.height(15.dp))

                                OutlinedTextField(
                                    value = note,
                                    onValueChange = {note=it},
                                    singleLine = false,
                                    label={Text("symptom")}
                                )

                                Spacer(Modifier.height(15.dp))

                                Button(
                                    onClick = {
                                        dbHelper.AddRecord(type,note, System.currentTimeMillis())
                                        showDialog=false
                                        newSymptom=false
                                    },
                                    modifier = Modifier.align(Alignment.End)){Text("Log",style = MaterialTheme.typography.labelSmall)}

                            }
                        }
                    }
                }

                if(newPrescription){
                    Dialog(onDismissRequest = {newPrescription=false}){
                        Card(colors= CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)){
                            Column(
                                modifier = Modifier
                                    .padding(vertical = 15.dp, horizontal = 15.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ){
                                var note by remember { mutableStateOf("") }

                                Text(
                                    "Enter new Prescription",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.align(Alignment.Start))


                                Spacer(Modifier.height(15.dp))

                                OutlinedTextField(
                                    value = note,
                                    onValueChange = {note=it},
                                    singleLine = false,
                                    label={Text("prescription")}
                                )

                                Spacer(Modifier.height(15.dp))

                                Button(
                                    onClick = {
                                        dbHelper.AddRecord(type,note, System.currentTimeMillis())
                                        showDialog=false
                                        newPrescription=false
                                    },
                                    modifier = Modifier.align(Alignment.End)){Text("Log",style = MaterialTheme.typography.labelSmall)}

                            }
                        }
                    }
                }

                if(imageupload){
                    var imagePath by remember {
                        mutableStateOf<String?>(null)
                    }

                    val launcher =
                        rememberLauncherForActivityResult(
                            ActivityResultContracts.GetContent()
                        ) {uri ->

                            uri?.let {
                                val fileName = "report_${System.currentTimeMillis()}.jpg"
                                val file = File(context.filesDir, fileName)

                                context.contentResolver.openInputStream(uri)?.use { input ->
                                    file.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }

                                dbHelper.AddRecord(
                                    "CONSULTATION REPORT",
                                    file.absolutePath,
                                    System.currentTimeMillis()
                                )
                                showDialog=false
                                imageupload=false
                            }
                        }
                    Dialog(onDismissRequest={imageupload=false}){
                        Card(colors= CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)){
                            Column(
                                modifier = Modifier
                                    .padding(vertical = 15.dp, horizontal = 15.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ){
                                Text(
                                    "Upload Consultation Image",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                                Spacer(Modifier.height(15.dp))
                                Button(
                                    onClick ={
                                        launcher.launch("image/*")
                                    }
                                ){Icon(Icons.Rounded.ImageSearch, contentDescription = "upload image")}
                            }
                        }
                    }
                }
            }

        }
    ){padding->

        Box(modifier = Modifier.fillMaxSize()) {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val records = dbHelper.getAll()

                item { Spacer(Modifier.height(15.dp)) }

                item {
                    Box(
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .fillMaxWidth(),
                    ) {
                        Text(
                            "RECORDS",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }

                item { Spacer(Modifier.height(50.dp)) }

                items(records) { record ->
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 5.dp)
                            .fillMaxWidth()
                            .clickable {
                                delAlert = true
                                idToDel = record.id
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(5.dp)) {

                            Text(
                                text = "> " + (record.type).lowercase(),
                                style = MaterialTheme.typography.titleLarge
                            )

                            Spacer(Modifier.height(15.dp))

                            when {
                                ("DOCTORS NOTE NEW SYMPTOM NEW PRESCRIPTION".contains(record.type)) -> {
                                    Text(
                                        record.data.toString(),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                (record.type.equals("FOLLOW UP")) -> {
                                    val temp = record.data?.toLong()?.toTime()?.dropLast(6)
                                    Text(
                                        "New follow up with doctor on $temp",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                (record.type.equals("CONSULTATION REPORT")) -> {
                                    AsyncImage(
                                        model = record.data,
                                        contentDescription = null
                                    )
                                }

                                else -> {
                                    Text(
                                        "Error Retrieving Data",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                            }
                            Spacer(Modifier.height(15.dp))
                            Row {
                                Spacer(Modifier.weight(1f))
                                Text(
                                    record.timestamp.toTime(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                        }
                    }
                    Spacer(Modifier.height(15.dp))

                }
            }

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
        }

    }

    if(delAlert){
        AlertDialog(
            onDismissRequest = {delAlert=false},
            dismissButton = {
                Button(onClick = {delAlert=false}){Text("Cancel",style = MaterialTheme.typography.labelSmall)}
            },
            confirmButton = {Button(onClick ={
                var deleted=dbHelper.delRec(idToDel)
                if(deleted!=0){
                    delAlert=false
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

fun Long.toTime(): String =
    SimpleDateFormat("dd MMM HH:mm", Locale.getDefault())
        .format(Date(this))