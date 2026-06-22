package com.example.meditrack.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.meditrack.screens.DocScreen
import com.example.meditrack.screens.VitalScreen
import com.example.meditrack.screens.signUpScreen
import com.example.meditrack.screens.loginScreen
import com.example.meditrack.screens.getInfoScreen
import com.google.firebase.auth.FirebaseAuth
import com.example.meditrack.screens.profileScreen
import com.example.meditrack.screens.reminderScreen

@Composable
fun AppNavigation(){
    val navController=rememberNavController()
    val auth= FirebaseAuth.getInstance()
    val startDes= if (auth.currentUser != null) "doctors" else "login"
    NavHost(
        navController=navController,
        startDestination = startDes
    ){
        composable("doctors"){DocScreen(navController)}
        composable("vitals"){VitalScreen(navController)}
        composable("signUp"){signUpScreen(navController)}
        composable("login"){loginScreen(navController)}
        composable("getInfo"){getInfoScreen(navController)}
        composable("profile"){profileScreen(navController)}
        composable("reminders"){reminderScreen(navController)}
    }
}