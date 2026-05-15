package com.example.quitsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.quitsync.navigation.NavGraph
import com.example.quitsync.navigation.Screen
import com.example.quitsync.ui.theme.QuitSyncTheme
import com.example.quitsync.viewmodel.AuthViewModel
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize Firebase at the very beginning
        FirebaseApp.initializeApp(this)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            QuitSyncTheme {
                val authViewModel: AuthViewModel = viewModel()
                val userRole by authViewModel.currentUserRole

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val showTopAndBottomBar = currentDestination?.route in listOf(
                    Screen.Home.route,
                    Screen.Community.route,
                    Screen.Journal.route,
                    Screen.TriggerMap.route,
                    Screen.Admin.route
                )

                val topBarTitle = when (currentDestination?.route) {
                    Screen.Home.route -> "QuitSync"
                    Screen.Community.route -> "Community"
                    Screen.Journal.route -> "Journal"
                    Screen.TriggerMap.route -> "Trigger Map"
                    Screen.Admin.route -> "Admin Panel"
                    Screen.Settings.route -> "Profile Settings"
                    else -> "QuitSync"
                }

                Scaffold(
                    topBar = {
                        if (showTopAndBottomBar) {
                            CenterAlignedTopAppBar(
                                title = { Text(topBarTitle, fontWeight = FontWeight.Bold) },
                                actions = {
                                    if (currentDestination?.route != Screen.Settings.route) {
                                        IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "Settings")
                                        }
                                    }
                                }
                            )
                        }
                    },
                    bottomBar = {
                        if (showTopAndBottomBar) {
                            NavigationBar {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                    label = { Text("Home") },
                                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Home.route } == true,
                                    onClick = {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    label = { Text("Community") },
                                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Community.route } == true,
                                    onClick = {
                                        navController.navigate(Screen.Community.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                    label = { Text("Journal") },
                                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Journal.route } == true,
                                    onClick = {
                                        navController.navigate(Screen.Journal.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                                    label = { Text("Map") },
                                    selected = currentDestination?.hierarchy?.any { it.route == Screen.TriggerMap.route } == true,
                                    onClick = {
                                        navController.navigate(Screen.TriggerMap.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                                // Only show Admin tab to admins
                                if (userRole == "admin") {
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                        label = { Text("Admin") },
                                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Admin.route } == true,
                                        onClick = {
                                            navController.navigate(Screen.Admin.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavGraph(
                        navController = navController,
                        authViewModel = authViewModel, // Pass the single source of truth
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
