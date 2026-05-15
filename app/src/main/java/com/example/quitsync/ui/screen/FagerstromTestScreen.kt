package com.example.quitsync.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quitsync.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FagerstromTestScreen(
    viewModel: AuthViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var q1Score by remember { mutableIntStateOf(-1) }
    var q2Score by remember { mutableIntStateOf(-1) }
    var q3Score by remember { mutableIntStateOf(-1) }
    var q4Score by remember { mutableIntStateOf(-1) }
    var q5Score by remember { mutableIntStateOf(-1) }
    var q6Score by remember { mutableIntStateOf(-1) }

    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val totalScore = listOf(q1Score, q2Score, q3Score, q4Score, q5Score, q6Score)
        .filter { it != -1 }
        .sum()

    val dependenceCategory = when (totalScore) {
        in 0..2 -> "Very Low Dependence"
        in 3..4 -> "Low Dependence"
        5 -> "Medium Dependence"
        in 6..7 -> "High Dependence"
        in 8..10 -> "Very High Dependence"
        else -> ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nicotine Dependence Test") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Fagerström Test for Nicotine Dependence",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            // Question 1
            FagerstromQuestion(
                question = "1. How soon after you wake up do you smoke your first cigarette?",
                options = listOf(
                    "Within 5 minutes" to 3,
                    "6-30 minutes" to 2,
                    "31-60 minutes" to 1,
                    "After 60 minutes" to 0
                ),
                selectedScore = q1Score,
                onScoreSelected = { q1Score = it }
            )

            // Question 2
            FagerstromQuestion(
                question = "2. Do you find it difficult to refrain from smoking in places where it is forbidden (e.g., in church, at the library, in cinema, etc.)?",
                options = listOf(
                    "Yes" to 1,
                    "No" to 0
                ),
                selectedScore = q2Score,
                onScoreSelected = { q2Score = it }
            )

            // Question 3
            FagerstromQuestion(
                question = "3. Which cigarette would you hate most to give up?",
                options = listOf(
                    "The first one in the morning" to 1,
                    "Any other" to 0
                ),
                selectedScore = q3Score,
                onScoreSelected = { q3Score = it }
            )

            // Question 4
            FagerstromQuestion(
                question = "4. How many cigarettes per day do you smoke?",
                options = listOf(
                    "10 or less" to 0,
                    "11-20" to 1,
                    "21-30" to 2,
                    "31 or more" to 3
                ),
                selectedScore = q4Score,
                onScoreSelected = { q4Score = it }
            )

            // Question 5
            FagerstromQuestion(
                question = "5. Do you smoke more frequently during the first hours after waking than during the rest of the day?",
                options = listOf(
                    "Yes" to 1,
                    "No" to 0
                ),
                selectedScore = q5Score,
                onScoreSelected = { q5Score = it }
            )

            // Question 6
            FagerstromQuestion(
                question = "6. Do you smoke if you are so ill that you are in bed most of the day?",
                options = listOf(
                    "Yes" to 1,
                    "No" to 0
                ),
                selectedScore = q6Score,
                onScoreSelected = { q6Score = it }
            )

            HorizontalDivider()

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Your Score: $totalScore", style = MaterialTheme.typography.titleMedium)
                    if (totalScore >= 0 && q1Score != -1 && q2Score != -1 && q3Score != -1 && q4Score != -1 && q5Score != -1 && q6Score != -1) {
                        Text("Category: $dependenceCategory", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Button(
                onClick = {
                    isSaving = true
                    viewModel.updateNicotineDependence(totalScore, dependenceCategory) { success, msg ->
                        isSaving = false
                        message = msg
                        if (success) {
                            Toast.makeText(context, "Test results saved successfully", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving && q1Score != -1 && q2Score != -1 && q3Score != -1 && q4Score != -1 && q5Score != -1 && q6Score != -1
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("Save Profile")
                }
            }

            message?.let {
                Text(it, color = if (it.contains("success", true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun FagerstromQuestion(
    question: String,
    options: List<Pair<String, Int>>,
    selectedScore: Int,
    onScoreSelected: (Int) -> Unit
) {
    Column {
        Text(text = question, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        options.forEach { (text, score) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (selectedScore == score),
                        onClick = { onScoreSelected(score) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (selectedScore == score),
                    onClick = null // Selected by Row onClick
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}
