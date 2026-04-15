package com.example.quitsync.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.quitsync.ui.screen.CommunityScreen
import com.example.quitsync.ui.screen.HomeScreen
import com.example.quitsync.ui.screen.JournalScreen
import com.example.quitsync.ui.screen.LoginScreen
import com.example.quitsync.ui.screen.PermissionScreen
import com.example.quitsync.ui.screen.SignUpScreen
import com.example.quitsync.ui.screen.TriggerMapScreen
import com.google.firebase.auth.FirebaseAuth

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Permissions : Screen("permissions")
    object Home : Screen("home")
    object Community : Screen("community")
    object Journal : Screen("journal")
    object TriggerMap : Screen("triggermap")
}

@Composable
fun NavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    // Use remember to avoid repeated calls to FirebaseAuth.getInstance() on recomposition
    val auth = remember { FirebaseAuth.getInstance() }

    // Logic fix: Logged in users go to Permissions first to ensure Geofencing works.
    // PermissionScreen will auto-navigate to Home if already granted.
    val startDestination = if (auth.currentUser != null) {
        Screen.Permissions.route
    } else {
        Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Permissions.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) }
            )
        }
        composable(Screen.SignUp.route) {
            SignUpScreen(onSignUpSuccess = {
                navController.navigate(Screen.Permissions.route) {
                    popUpTo(Screen.SignUp.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Permissions.route) {
            PermissionScreen(onAllPermissionsGranted = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Permissions.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Home.route) {
            HomeScreen()
        }
        composable(Screen.Community.route) {
            CommunityScreen()
        }
        composable(Screen.Journal.route) {
            JournalScreen()
        }
        composable(Screen.TriggerMap.route) {
            TriggerMapScreen()
        }
    }
}
