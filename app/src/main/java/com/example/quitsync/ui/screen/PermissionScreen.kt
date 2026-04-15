package com.example.quitsync.ui.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun PermissionScreen(onAllPermissionsGranted: () -> Unit) {
    val context = LocalContext.current

    var fineLocationGranted by remember {
        mutableStateOf(checkPermission(context, Manifest.permission.ACCESS_FINE_LOCATION))
    }
    var backgroundLocationGranted by remember {
        mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            checkPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            true
        })
    }
    var notificationsGranted by remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mutableStateOf(checkPermission(context, Manifest.permission.POST_NOTIFICATIONS))
        } else {
            mutableStateOf(true)
        }
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        backgroundLocationGranted = isGranted
    }

    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: fineLocationGranted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: notificationsGranted
        }
    }

    LaunchedEffect(fineLocationGranted, backgroundLocationGranted, notificationsGranted) {
        if (fineLocationGranted && backgroundLocationGranted && notificationsGranted) {
            onAllPermissionsGranted()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "QuitSync Location Access",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "To protect you from trigger zones, we need location and notification permissions.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Step 1: Foreground Location & Notifications
        PermissionStep(
            title = "1. Basic Permissions",
            description = "Allows the app to see your location while open and send alerts.",
            isGranted = fineLocationGranted && notificationsGranted,
            onClick = {
                val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                multiplePermissionLauncher.launch(permissions.toTypedArray())
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step 2: Background Location (Android 10+)
        if (fineLocationGranted) {
            PermissionStep(
                title = "2. Background Access",
                description = "Required to alert you even when the app is closed. Select 'Allow all the time'.",
                isGranted = backgroundLocationGranted,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    } else {
                        backgroundLocationGranted = true
                    }
                }
            )
        }
    }
}

@Composable
fun PermissionStep(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = description, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onClick,
                enabled = !isGranted,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(if (isGranted) "Granted" else "Grant")
            }
        }
    }
}

private fun checkPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
