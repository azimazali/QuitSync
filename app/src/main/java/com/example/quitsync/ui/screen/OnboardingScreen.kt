package com.example.quitsync.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.example.quitsync.ui.components.FagerstromTestContent
import com.example.quitsync.ui.components.calculateFagerstromResults
import com.example.quitsync.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import java.util.Date

@Composable
fun OnboardingScreen(
    viewModel: AuthViewModel = viewModel(),
    onOnboardingComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = false // Control navigation via buttons
            ) { page ->
                when (page) {
                    0 -> DependencePage(viewModel)
                    1 -> GoalsPage(viewModel)
                    2 -> FinancialPage(viewModel, onComplete = {
                        viewModel.completeOnboarding { success, msg ->
                            if (success) {
                                onOnboardingComplete()
                            } else {
                                Toast.makeText(context, msg ?: "Failed to complete setup", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                }
            }

            // Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (pagerState.currentPage < 2) {
                    Button(onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }) {
                        Text("Next")
                    }
                }
            }
        }
    }
}

@Composable
fun DependencePage(viewModel: AuthViewModel) {
    var q1Score by remember { mutableIntStateOf(-1) }
    var q2Score by remember { mutableIntStateOf(-1) }
    var q3Score by remember { mutableIntStateOf(-1) }
    var q4Score by remember { mutableIntStateOf(-1) }
    var q5Score by remember { mutableIntStateOf(-1) }
    var q6Score by remember { mutableIntStateOf(-1) }

    val (totalScore, dependenceCategory) = calculateFagerstromResults(
        listOf(q1Score, q2Score, q3Score, q4Score, q5Score, q6Score)
    )

    // Update VM whenever values change
    LaunchedEffect(q1Score, q2Score, q3Score, q4Score, q5Score, q6Score) {
        if (q1Score != -1 && q2Score != -1 && q3Score != -1 && q4Score != -1 && q5Score != -1 && q6Score != -1) {
            viewModel.updateNicotineDependence(totalScore, dependenceCategory) { _, _ -> }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Step 1: Dependence", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Find out how much your body depends on nicotine.", style = MaterialTheme.typography.bodyMedium)

        FagerstromTestContent(
            q1Score = q1Score, onQ1Selected = { q1Score = it },
            q2Score = q2Score, onQ2Selected = { q2Score = it },
            q3Score = q3Score, onQ3Selected = { q3Score = it },
            q4Score = q4Score, onQ4Selected = { q4Score = it },
            q5Score = q5Score, onQ5Selected = { q5Score = it },
            q6Score = q6Score, onQ6Selected = { q6Score = it }
        )
    }
}

@Composable
fun GoalsPage(viewModel: AuthViewModel) {
    val goalOptions = listOf(
        "Save Money",
        "Improve Health",
        "Whiter Teeth",
        "Live Longer",
        "Mental Clarity"
    )
    val selectedGoals = remember { mutableStateListOf<String>() }
    var customGoalText by remember { mutableStateOf("") }

    // Initialize from VM if exists
    LaunchedEffect(Unit) {
        viewModel.currentUserData.value?.goals?.let {
            selectedGoals.clear()
            selectedGoals.addAll(it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Step 2: Your Goals", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Why do you want to quit? Select all that apply.", style = MaterialTheme.typography.bodyMedium)

        // Combine predefined and any custom goals that are already selected
        val allDisplayGoals = (goalOptions + selectedGoals).distinct()

        allDisplayGoals.forEach { goal ->
            val isSelected = selectedGoals.contains(goal)
            FilterChip(
                selected = isSelected,
                onClick = {
                    if (isSelected) selectedGoals.remove(goal) else selectedGoals.add(goal)
                    viewModel.updateGoals(selectedGoals.toList()) { _, _ -> }
                },
                label = { Text(goal) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Custom Goal Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customGoalText,
                onValueChange = { customGoalText = it },
                label = { Text("Add custom goal...") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(
                onClick = {
                    if (customGoalText.isNotBlank() && !selectedGoals.contains(customGoalText)) {
                        selectedGoals.add(customGoalText)
                        viewModel.updateGoals(selectedGoals.toList()) { _, _ -> }
                        customGoalText = ""
                    }
                },
                enabled = customGoalText.isNotBlank()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal")
            }
        }
    }
}

@Composable
fun FinancialPage(viewModel: AuthViewModel, onComplete: () -> Unit) {
    var pricePerPack by remember { mutableStateOf("") }
    var dailyCigarettes by remember { mutableStateOf("") }
    var cigarettesPerPack by remember { mutableStateOf("20") }
    var isSaving by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Step 3: Financial Impact", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("We'll use this to calculate how much you're saving.", style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = pricePerPack,
            onValueChange = { input ->
                if (input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                    pricePerPack = input
                }
            },
            label = { Text("Price per pack (e.g. 17.50)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = dailyCigarettes,
            onValueChange = { input ->
                if (input.all { it.isDigit() }) {
                    dailyCigarettes = input
                }
            },
            label = { Text("Cigarettes smoked per day") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = cigarettesPerPack,
            onValueChange = { input ->
                if (input.all { it.isDigit() }) {
                    cigarettesPerPack = input
                }
            },
            label = { Text("Cigarettes per pack") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val price = pricePerPack.toDoubleOrNull() ?: 0.0
                val dailyCount = dailyCigarettes.toIntOrNull() ?: 0
                val perPack = cigarettesPerPack.toIntOrNull() ?: 20

                isSaving = true
                viewModel.updateFinancialData(price, dailyCount, perPack, Date()) { success, _ ->
                    if (success) {
                        onComplete()
                    } else {
                        isSaving = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving && pricePerPack.isNotBlank() && dailyCigarettes.isNotBlank()
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text("Start My Journey")
            }
        }
    }
}
