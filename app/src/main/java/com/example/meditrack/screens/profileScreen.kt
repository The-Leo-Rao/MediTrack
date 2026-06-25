package com.example.meditrack.screens

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.example.meditrack.DBHelper
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun profileScreen(navController: NavController){
    var showInfo by remember { mutableStateOf(false) }
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
        Box {
            val auth = FirebaseAuth.getInstance()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var editName by remember { mutableStateOf(false) }
                var editBlood by remember { mutableStateOf(false) }
                var editChronic by remember { mutableStateOf(false) }
                var editAllergies by remember { mutableStateOf(false) }
                var editEmergency by remember { mutableStateOf(false) }

                var name: String? by remember { mutableStateOf("") }
                var bloodG: String? by remember { mutableStateOf("") }
                var chronic: String? by remember { mutableStateOf("") }
                var allergies: String? by remember { mutableStateOf("") }
                var emergency: String? by remember { mutableStateOf("") }

                val user = FirebaseAuth.getInstance().currentUser
                val email = user?.email?.substringBefore("@")
                val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

                val fstore = FirebaseFirestore.getInstance()
                user?.uid?.let {
                    fstore.collection("users")
                        .document(it)
                        .get()
                        .addOnSuccessListener { document ->
                            name = document.getString("name")
                            bloodG = document.getString("Blood-Group")
                            chronic = document.getString("Chronic illnesses")
                            allergies = document.getString("Allergies")
                            emergency = document.getString("Emergency")
                        }
                }
                Spacer(Modifier.height(15.dp))

                Text(
                    "ACCOUNT",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.height(50.dp))

                Card(
                    modifier = Modifier.padding(horizontal = 15.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Name: $name")
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { editName = true }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null
                            )
                        }
                    }
                }

                Spacer(Modifier.height(15.dp))

                Card(
                    modifier = Modifier.padding(horizontal = 15.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Blood Group: $bloodG")
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { editBlood = true }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null
                            )
                        }
                    }
                }

                Spacer(Modifier.height(15.dp))

                Card(
                    modifier = Modifier.padding(horizontal = 15.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Chronic Illnesses: $chronic")
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { editChronic = true }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null
                            )
                        }
                    }
                }

                Spacer(Modifier.height(15.dp))

                Card(
                    modifier = Modifier.padding(horizontal = 15.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Allergies: $allergies")
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { editAllergies = true }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null
                            )
                        }
                    }
                }

                Spacer(Modifier.height(15.dp))

                Card(
                    modifier = Modifier.padding(horizontal = 15.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Emergency contact: $emergency")
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { editEmergency = true }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null
                            )
                        }
                    }
                }

                Spacer(Modifier.height(25.dp))

                Button(
                    onClick = {
                        auth.signOut()
                        navController.navigate("login"){
                            popUpTo(0)
                        }
                    }
                ) {
                    Text(
                    text="Log Out",
                    style= MaterialTheme.typography.bodyMedium
                ) }



                if (editName) {
                    var newname by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { editName = false },
                        confirmButton = {
                            Button(
                                onClick = {
                                    user?.uid?.let {
                                        fstore.collection("users")
                                            .document(it)
                                            .update("name", newname)
                                    }
                                    editName = false

                                }) { Text("Confirm", style = MaterialTheme.typography.labelSmall) }
                        },

                        title = { Text("Edit Name", style = MaterialTheme.typography.titleLarge) },
                        text = {
                            OutlinedTextField(
                                value = newname,
                                onValueChange = { newname = it },
                                singleLine = true,
                                label = { Text("Name") }
                            )
                        }
                    )
                }

                if (editBlood) {
                    var newname by remember { mutableStateOf("") }
                    var expanded by remember { mutableStateOf(false) }
                    AlertDialog(
                        onDismissRequest = { editBlood = false },
                        confirmButton = {
                            Button(
                                onClick = {
                                    user?.uid?.let {
                                        fstore.collection("users")
                                            .document(it)
                                            .update("Blood-Group", newname)
                                    }
                                    editBlood = false

                                }) { Text("Confirm", style = MaterialTheme.typography.labelSmall) }
                        },

                        title = {
                            Text(
                                "Edit Blood Group",
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        text = {
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = newname,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = {
                                        Text(
                                            "Select Blood Group",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = expanded
                                        )
                                    },
                                    modifier = Modifier.menuAnchor()
                                )

                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    bloodGroups.forEach { bgrp ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    bgrp,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            },
                                            onClick = {
                                                newname = bgrp
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

                if (editAllergies) {
                    var newname by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { editAllergies = false },
                        confirmButton = {
                            Button(
                                onClick = {
                                    user?.uid?.let {
                                        fstore.collection("users")
                                            .document(it)
                                            .update("Allergies", newname)
                                    }
                                    editAllergies = false

                                }) { Text("Confirm", style = MaterialTheme.typography.labelSmall) }
                        },

                        title = {
                            Text(
                                "Edit Allergies",
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        text = {
                            OutlinedTextField(
                                value = newname,
                                onValueChange = { newname = it },
                                singleLine = true,
                                label = { Text("Allergies") }
                            )
                        }
                    )
                }

                if (editChronic) {
                    var newname by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { editChronic = false },
                        confirmButton = {
                            Button(
                                onClick = {
                                    user?.uid?.let {
                                        fstore.collection("users")
                                            .document(it)
                                            .update("Chronic illnesses", newname)
                                    }
                                    editChronic = false

                                }) { Text("Confirm", style = MaterialTheme.typography.labelSmall) }
                        },

                        title = {
                            Text(
                                "Edit chronic illness",
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        text = {
                            OutlinedTextField(
                                value = newname,
                                onValueChange = { newname = it },
                                singleLine = true,
                                label = { Text("") }
                            )
                        }
                    )
                }

                if (editEmergency) {
                    var newname by remember { mutableStateOf("") }
                    var errM by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { editEmergency = false },
                        confirmButton = {
                            Button(
                                onClick = {
                                    when{
                                        newname.isBlank()->{errM="Name cannot be Blank"}
                                        newname.length!=10->{errM="Invalid number"}

                                        else->{
                                            user?.uid?.let {
                                                fstore.collection("users")
                                                    .document(it)
                                                    .update("Emergency", newname)
                                            }
                                            editEmergency = false
                                        }
                                    }
                                }) { Text("Confirm", style = MaterialTheme.typography.labelSmall) }
                        },

                        title = { Text("Edit emergency contact", style = MaterialTheme.typography.titleLarge) },
                        text = {
                            OutlinedTextField(
                                value = newname,
                                onValueChange = { newname = it },
                                singleLine = true,
                                prefix = {Text("+91", style = MaterialTheme.typography.labelMedium)}
                            )

                            Spacer(Modifier.height(15.dp))

                            Text(errM)
                        }
                    )
                }
            }

            Button(
                onClick = {showInfo=true},
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 20.dp,top=40.dp),
                    shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0,0,0,0)
                )
            ) {Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)}
        }
    }

    if(showInfo){
        val context= LocalContext.current
        val dbHelper= DBHelper(context)
        AlertDialog(
            onDismissRequest = {showInfo=false},
            confirmButton = {},
            title = {Text("App Info",style= MaterialTheme.typography.bodyLarge)},
            text={
                LazyColumn(
                    modifier = Modifier
                        .padding(15.dp)
                        .height(250.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    item{Text("MediTrack is a personal health management application designed to help users record, monitor, and visualize important health metrics over time.",style= MaterialTheme.typography.bodyMedium)}

                    item{Spacer(Modifier.height(25.dp))}

                    item{Text("The SOS feature is designed to help users quickly notify their emergency contact. for activating SOS, press and hold the SOS button for 3 seconds. A message will be automatically sent to your emergency contact with your location and health data")}

                    item{Spacer(Modifier.height(25.dp))}

                    item{Text("Disclaimer", style = MaterialTheme.typography.titleLarge,modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)}

                    item{Spacer(Modifier.height(15.dp))}

                    item{Text("MediTrack is intended for informational and personal record-keeping purposes only. The application does not provide medical advice, diagnosis, or treatment. Always consult a qualified healthcare professional regarding medical concerns or decisions.")}

                    item{Spacer(Modifier.height(15.dp))}

                    item{Button(onClick = { dbHelper.seedDemoVitals() }) {
                        Text("Seed Test Data", style = MaterialTheme.typography.labelSmall)
                    }}
                }
            }
        )
    }
}