package com.example.meditrack.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.meditrack.NotificationHelper
import kotlinx.coroutines.delay

@Composable
fun VitalScreen(navController: NavController){
    val context = LocalContext.current
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
                                .height(175.dp),
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
                                .height(175.dp),
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
                                .aspectRatio(1f),
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
                                .aspectRatio(1f),
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
                                .aspectRatio(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(15.dp)
                            ) {
                                Text(
                                    "BPM",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.align(Alignment.TopStart)
                                )
                            }
                        }
                        Spacer(Modifier.width(15.dp))
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
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
                            .height(60.dp),
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
    }
}