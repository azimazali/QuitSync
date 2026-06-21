package com.example.quitsync.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import com.example.quitsync.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GoalSettingScreen(
    viewModel: AuthViewModel,
    onNavigateBack: (() -> Unit)? = null,
    onGoalsSaved: () -> Unit
) {
    val context = LocalContext.current
    val commonGoals = listOf(
        "Save Money",
        "Improve Health",
        "Whiter Teeth",
        "Live Longer",
        "Mental Clarity"
    )

    val selectedGoals = remember { mutableStateListOf<String>() }
    var customGoal by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Initialize from VM
    LaunchedEffect(Unit) {
        viewModel.currentUserData.value?.goals?.let {
            selectedGoals.clear()
            selectedGoals.addAll(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Your Goals") },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Why do you want to quit?",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Select all the reasons that motivate you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                commonGoals.forEach { goal ->
                    val isSelected = selectedGoals.contains(goal)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) selectedGoals.remove(goal) else selectedGoals.add(goal)
                        },
                        label = { Text(goal) }
                    )
                }

                // Show already selected custom goals that aren't in commonGoals
                selectedGoals.filter { it !in commonGoals }.forEach { goal ->
                    FilterChip(
                        selected = true,
                        onClick = { selectedGoals.remove(goal) },
                        label = { Text(goal) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = customGoal,
                    onValueChange = { customGoal = it },
                    label = { Text("Add Custom Goal") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (customGoal.isNotBlank() && !selectedGoals.contains(customGoal.trim())) {
                            selectedGoals.add(customGoal.trim())
                            customGoal = ""
                        }
                    },
                    enabled = customGoal.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = {
                    isLoading = true
                    viewModel.updateGoals(selectedGoals.toList()) { success, message ->
                        isLoading = false
                        if (success) {
                            Toast.makeText(context, "Goals updated successfully!", Toast.LENGTH_SHORT).show()
                            onGoalsSaved()
                        } else {
                            errorMessage = message
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && selectedGoals.isNotEmpty()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Goals")
                }
            }
        }
    }
}
