package com.example.quitsync.ui.screen

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quitsync.viewmodel.AuthViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AuthViewModel = viewModel(),
    onNavigateToNicotineTest: () -> Unit,
    onNavigateToGoalSetting: () -> Unit,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val userData by viewModel.currentUserData
    var cigarettePrice by remember { mutableStateOf("") }
    var dailyCigarettes by remember { mutableStateOf("") }
    var cigarettesPerPack by remember { mutableStateOf("") }
    var quitDate by remember { mutableStateOf(Date()) }
    var newPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    val scrollState = rememberScrollState()

    // Initialize fields from user data
    LaunchedEffect(userData) {
        userData?.let {
            cigarettePrice = it.cigarettePrice.toString()
            dailyCigarettes = it.dailyCigarettes.toString()
            cigarettesPerPack = it.cigarettesPerPack.toString()
            it.quitDate?.let { date -> quitDate = date }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section: Financial Tracking
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Quit Progress & Savings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = cigarettePrice,
                        onValueChange = { cigarettePrice = it },
                        label = { Text("Price per Pack (RM)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = dailyCigarettes,
                        onValueChange = { dailyCigarettes = it },
                        label = { Text("Cigarettes Smoked per Day") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = cigarettesPerPack,
                        onValueChange = { cigarettesPerPack = it },
                        label = { Text("Cigarettes per Pack") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Quit Date:", style = MaterialTheme.typography.labelMedium)
                    OutlinedButton(
                        onClick = {
                            val calendar = Calendar.getInstance().apply { time = quitDate }
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val newDate = Calendar.getInstance().apply {
                                        set(year, month, day)
                                    }.time
                                    quitDate = newDate
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = sdf.format(quitDate))
                    }

                    Button(
                        onClick = {
                            val price = cigarettePrice.toDoubleOrNull() ?: 0.0
                            val count = dailyCigarettes.toIntOrNull() ?: 0
                            val perPack = cigarettesPerPack.toIntOrNull() ?: 20
                            viewModel.updateFinancialData(price, count, perPack, quitDate) { success, msg ->
                                if (success) {
                                    Toast.makeText(context, "Settings updated successfully", Toast.LENGTH_SHORT).show()
                                    message = null
                                } else {
                                    message = msg
                                    isError = true
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.End).padding(top = 16.dp)
                    ) {
                        Text("Update Data")
                    }
                }
            }

            // Section: Nicotine Dependence Test
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Nicotine Dependence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    userData?.let { user ->
                        if (user.nicotineDependenceCategory.isNotEmpty()) {
                            Text(text = "Last Score: ${user.nicotineDependenceScore}/10", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Category: ${user.nicotineDependenceCategory}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                        } else {
                            Text(text = "Take the test to assess your nicotine dependence.", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Button(
                        onClick = onNavigateToNicotineTest,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Assignment, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (userData?.nicotineDependenceCategory?.isEmpty() == true) "Take Test" else "Retake Test")
                    }
                }
            }

            // Section: My Journey Goals
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "My Journey Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    userData?.let { user ->
                        if (user.goals.isNotEmpty()) {
                            Text(text = "Current Goals:", style = MaterialTheme.typography.labelMedium)
                            user.goals.forEach { goal ->
                                Text(text = "• $goal", style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        } else {
                            Text(text = "Set your goals to stay motivated.", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Button(
                        onClick = onNavigateToGoalSetting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Flag, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (userData?.goals?.isEmpty() == true) "Set Goals" else "Update Goals")
                    }
                }
            }

            // Section: Security
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (newPassword.length >= 6) {
                                viewModel.changePassword(newPassword) { success, msg ->
                                    if (success) {
                                        Toast.makeText(context, "Password changed successfully", Toast.LENGTH_SHORT).show()
                                        message = null
                                        newPassword = ""
                                    } else {
                                        message = msg
                                        isError = true
                                    }
                                }
                            } else {
                                message = "Password must be at least 6 characters"
                                isError = true
                            }
                        },
                        modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                    ) {
                        Text("Change Password")
                    }
                }
            }

            if (message != null) {
                Text(
                    text = message!!,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    viewModel.logout()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Logout", color = Color.White)
            }

            TextButton(
                onClick = { showDeleteConfirmation = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Account", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to delete your account? This action is permanent and all your data will be lost.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteAccount { success, msg ->
                            if (success) {
                                onLogout() // Navigate to login
                            } else {
                                message = msg
                                isError = true
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
