package com.example.quitsync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp
import android.content.pm.PackageManager
import androidx.compose.ui.Alignment
import com.example.quitsync.ui.components.ShowcaseOverlay
import com.example.quitsync.ui.components.showcaseTarget

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize Firebase at the very beginning
        FirebaseApp.initializeApp(this)

        // Initialize Places API
        if (!Places.isInitialized()) {
            val apiKey = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                .metaData.getString("com.google.android.geo.API_KEY") ?: ""
            Places.initialize(applicationContext, apiKey)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            QuitSyncTheme {
                val authViewModel: AuthViewModel = viewModel()
                val userData by authViewModel.currentUserData
                val userRole by authViewModel.currentUserRole

                // Showcase State
                var currentTourStep by remember(userData?.uid) { mutableIntStateOf(0) }
                val hasSeenTour = userData?.hasSeenTour ?: true
                val showcaseTargets = authViewModel.showcaseTargets

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

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        topBar = {
                            if (showTopAndBottomBar) {
                                CenterAlignedTopAppBar(
                                    title = { Text(topBarTitle, fontWeight = FontWeight.Bold) },
                                    actions = {
                                        if (currentDestination?.route != Screen.Settings.route) {
                                            IconButton(
                                                onClick = { navController.navigate(Screen.Settings.route) },
                                                modifier = Modifier.showcaseTarget("overflow_menu") { tag, rect ->
                                                    authViewModel.updateShowcaseTarget(tag, rect)
                                                }
                                            ) {
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
                            authViewModel = authViewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                    // Showcase Overlay (at absolute root)
                    if (!hasSeenTour && showTopAndBottomBar) {
                        val tourStepData = listOf(
                            Triple(Screen.Home.route, "overflow_menu", "Tap here to update your profile and security settings."),
                            Triple(Screen.Home.route, "financial_card", "Track your daily smoke-free progress and financial savings here."),
                            Triple(Screen.Community.route, "community_post", "Share your journey and get support from the community here."),
                            Triple(Screen.Journal.route, "journal_add", "Document your feelings and triggers to better understand your habits."),
                            Triple(Screen.TriggerMap.route, "map_zones", "Manage your high-risk trigger zones and stay alert.")
                        )

                        if (currentTourStep < tourStepData.size) {
                            val (targetRoute, targetTag, tooltipText) = tourStepData[currentTourStep]
                            
                            // Auto-navigate if needed
                            LaunchedEffect(targetRoute) {
                                if (currentDestination?.route != targetRoute) {
                                    navController.navigate(targetRoute) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }

                            ShowcaseOverlay(
                                isVisible = true, // Always visible while tour is active
                                currentStep = currentTourStep,
                                targetRect = showcaseTargets[targetTag],
                                text = tooltipText,
                                isLastStep = currentTourStep == tourStepData.size - 1,
                                onNext = {
                                    if (currentTourStep < tourStepData.size - 1) {
                                        currentTourStep++
                                    } else {
                                        authViewModel.completeTour()
                                    }
                                },
                                onDismiss = {
                                    authViewModel.completeTour()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
