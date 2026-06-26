package com.example.meditrack.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.exp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun getInfoScreen(navController: NavController){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(25.dp)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        val focusManager = LocalFocusManager.current
        var errMsg by remember { mutableStateOf("") }
        var age by remember { mutableStateOf("") }
        var allergies by remember{mutableStateOf("")}
        var chronic by remember{mutableStateOf("")}
        var expanded by remember { mutableStateOf(false) }
        var selectedBloodG by remember{mutableStateOf("")}
        var emergency by remember { mutableStateOf("") }


        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

        Text("Enter Additional Data")

        Spacer(Modifier.height(25.dp))

        OutlinedTextField(
            value = age,
            onValueChange = {age=it},
            singleLine = true,
            label = {Text("Enter Age")}
        )

        Spacer(Modifier.height(25.dp))

        ExposedDropdownMenuBox(
            expanded=expanded,
            onExpandedChange = {expanded=!expanded}
        ){
            OutlinedTextField(
                value = selectedBloodG,
                onValueChange = {},
                readOnly = true,
                label = {Text("Select Blood Group")},
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded=expanded)},
                modifier = Modifier.menuAnchor()
            )

            ExposedDropdownMenu(
                expanded=expanded,
                onDismissRequest = {expanded=false}
            ){
                bloodGroups.forEach{bgrp->
                    DropdownMenuItem(
                        text={Text(bgrp)},
                        onClick ={
                            selectedBloodG=bgrp
                            expanded=false}
                    )
                }
            }
        }

        Spacer(Modifier.height(15.dp))

        OutlinedTextField(
            value = allergies,
            onValueChange = {allergies=it},
            singleLine = true,
            label = {Text("Enter Allergies if any")}
        )

        Spacer(Modifier.height(15.dp))

        OutlinedTextField(
            value = chronic,
            onValueChange = {chronic=it},
            singleLine = true,
            label={Text("Chronic Illnesses if any")}
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = emergency,
            onValueChange = {
                if (it.all(Char::isDigit) && it.length <= 10) {
                    emergency = it
                }
            },
            singleLine = true,
            label = { Text("Enter emergency contact") },
            prefix = {
                Text(
                    "+91",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        )


        Spacer(Modifier.height(10.dp))

        Text(errMsg)

        Spacer(Modifier.height(15.dp))

        Button(
            onClick = {
                when{
                    emergency.isBlank()->{errMsg="Emergency contact is empty"}
                    emergency.length<10->{errMsg="Invalid emergency number"}
                    ((age.toInt()>110)||(age.toInt()<1))->{errMsg="Invalid Age"}

                    else->{
                        focusManager.clearFocus()

                        val uid=auth.currentUser!!.uid
                        val user = FirebaseAuth.getInstance().currentUser
                        db.collection("users")
                            .document(uid)
                            .set(
                                mapOf(
                                    "Age" to age,
                                    "Blood-Group" to selectedBloodG,
                                    "Allergies" to allergies,
                                    "Chronic illnesses" to chronic,
                                    "name" to user?.email?.substringBefore("@"),
                                    "Emergency" to emergency
                                )
                            )
                            .addOnSuccessListener{navController.navigate("doctors"){popUpTo("getInfo"){inclusive=true} } }
                            .addOnFailureListener{errMsg=it.message?:"Error signing up"}
                    }
                }
            }
        ){
            Text("Continue")
        }


    }
}