package com.example.quitsync.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.quitsync.ui.screen.AdminScreen
import com.example.quitsync.ui.screen.CommunityScreen
import com.example.quitsync.ui.screen.FagerstromTestScreen
import com.example.quitsync.ui.screen.HomeScreen
import com.example.quitsync.ui.screen.JournalScreen
import com.example.quitsync.ui.screen.LoginScreen
import com.example.quitsync.ui.screen.PermissionScreen
import com.example.quitsync.ui.screen.SettingsScreen
import com.example.quitsync.ui.screen.SignUpScreen
import com.example.quitsync.ui.screen.TriggerMapScreen
import com.example.quitsync.viewmodel.AuthViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Permissions : Screen("permissions")
    object Home : Screen("home")
    object Community : Screen("community")
    object Journal : Screen("journal")
    object TriggerMap : Screen("triggermap")
    object Admin : Screen("admin")
    object Settings : Screen("settings")
    object FagerstromTest : Screen("fagerstrom_test")
    object GoalSetting : Screen("goal_setting")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val startDestination = if (authViewModel.isUserLoggedIn.value) {
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
                viewModel = authViewModel,
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
            val prevRoute = navController.previousBackStackEntry?.destination?.route
            PermissionScreen(onAllPermissionsGranted = {
                val nextScreen = if (prevRoute == Screen.SignUp.route) {
                    Screen.GoalSetting.route
                } else {
                    Screen.Home.route
                }
                navController.navigate(nextScreen) {
                    popUpTo(Screen.Permissions.route) { inclusive = true }
                }
            })
        }
        composable(Screen.GoalSetting.route) {
            val fromOnboarding = navController.previousBackStackEntry?.destination?.route == Screen.Permissions.route
            com.example.quitsync.ui.screen.GoalSettingScreen(
                viewModel = authViewModel,
                onNavigateBack = if (fromOnboarding) null else { { navController.popBackStack() } },
                onGoalsSaved = {
                    if (fromOnboarding) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.GoalSetting.route) { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen()
        }
        composable(Screen.Community.route) {
            CommunityScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Journal.route) {
            JournalScreen()
        }
        composable(Screen.TriggerMap.route) {
            TriggerMapScreen()
        }
        composable(Screen.Admin.route) {
            AdminScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = authViewModel,
                onNavigateToNicotineTest = { navController.navigate(Screen.FagerstromTest.route) },
                onNavigateToGoalSetting = { navController.navigate(Screen.GoalSetting.route) },
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.FagerstromTest.route) {
            FagerstromTestScreen(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
