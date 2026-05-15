package com.example.quitsync.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.quitsync.ui.screen.*
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
    object Onboarding : Screen("onboarding")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val userData = authViewModel.currentUserData.value
    val isLoading = authViewModel.isUserDataLoading.value
    val isLoggedIn = authViewModel.isUserLoggedIn.value

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val startDestination = if (isLoggedIn) {
            if (userData != null && !userData.hasCompletedSetup) {
                Screen.Onboarding.route
            } else {
                Screen.Permissions.route
            }
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
                        // Re-evaluation of NavHost will happen, but we can also force it
                        val next = if (userData != null && !userData.hasCompletedSetup) Screen.Onboarding.route else Screen.Permissions.route
                        navController.navigate(next) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) }
                )
            }
            composable(Screen.SignUp.route) {
                SignUpScreen(
                    viewModel = authViewModel,
                    onSignUpSuccess = {
                        // For a new user, setup is always false initially
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.SignUp.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    viewModel = authViewModel,
                    onOnboardingComplete = {
                        navController.navigate(Screen.Permissions.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Permissions.route) {
                PermissionScreen(onAllPermissionsGranted = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Permissions.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.GoalSetting.route) {
                GoalSettingScreen(
                    viewModel = authViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onGoalsSaved = { navController.popBackStack() }
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
}
