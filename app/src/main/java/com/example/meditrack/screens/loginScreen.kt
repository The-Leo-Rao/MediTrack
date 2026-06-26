package com.example.meditrack.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.google.firebase.auth.FirebaseAuth

@Composable
fun loginScreen(navController: NavController){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(25.dp)
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        val focusManager= LocalFocusManager.current
        val auth = FirebaseAuth.getInstance()

        var email by remember{ mutableStateOf("")}
        var password by remember{mutableStateOf("")}
        var passwordVisible by remember {mutableStateOf(false) }
        var errMsg by remember{mutableStateOf("")}

        Text("Login")

        Spacer(Modifier.height(25.dp))

        OutlinedTextField(
            value=email,
            onValueChange = {email=it},
            label = {Text("Email")},
            singleLine = true
        )

        Spacer(Modifier.height(15.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {password=it},
            label={Text("Password")},
            singleLine = true,
            visualTransformation =
                if(passwordVisible){VisualTransformation.None}
                else{PasswordVisualTransformation()},
            trailingIcon={
                IconButton(onClick ={passwordVisible=!passwordVisible}){
                    Icon(
                        imageVector =
                            if(passwordVisible){Icons.Default.Visibility}
                            else{Icons.Default.VisibilityOff},
                        contentDescription = null
                    )
                }
            }
        )

        Spacer(Modifier.height(15.dp))

        Text(errMsg)

        Spacer(Modifier.height(15.dp))

        Button(
            onClick ={
                focusManager.clearFocus()
                when{
                    email.isBlank()->{errMsg="Email cannot be empty"}
                    password.isBlank()->{errMsg="Password cannot be empty"}

                    else->{
                        auth.signInWithEmailAndPassword(email,password)
                            .addOnCompleteListener{task->
                                if(task.isSuccessful){navController.navigate("doctors"){popUpTo(0)}}
                                else{errMsg="Invalid credentials"}
                            }
                    }
                }

            }
        ){
            Text("Login")
        }

        Spacer(Modifier.height(10.dp))

        TextButton(
            onClick = { navController.navigate("forgotp") },
        ) {
            Text("forgot password?"
                ,style= MaterialTheme.typography.labelMedium)
        }

        Spacer(Modifier.height(25.dp))

        Text(
            "New to MediTrack?",
            modifier = Modifier.clickable{navController.navigate("signUp")}
        )
    }
}