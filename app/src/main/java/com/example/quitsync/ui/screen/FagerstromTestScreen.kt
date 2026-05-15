package com.example.quitsync.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quitsync.ui.components.FagerstromTestContent
import com.example.quitsync.ui.components.calculateFagerstromResults
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

    val (totalScore, dependenceCategory) = calculateFagerstromResults(
        listOf(q1Score, q2Score, q3Score, q4Score, q5Score, q6Score)
    )

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

            FagerstromTestContent(
                q1Score = q1Score, onQ1Selected = { q1Score = it },
                q2Score = q2Score, onQ2Selected = { q2Score = it },
                q3Score = q3Score, onQ3Selected = { q3Score = it },
                q4Score = q4Score, onQ4Selected = { q4Score = it },
                q5Score = q5Score, onQ5Selected = { q5Score = it },
                q6Score = q6Score, onQ6Selected = { q6Score = it }
            )

            HorizontalDivider()

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Your Score: $totalScore", style = MaterialTheme.typography.titleMedium)
                    if (q1Score != -1 && q2Score != -1 && q3Score != -1 && q4Score != -1 && q5Score != -1 && q6Score != -1) {
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
