package com.example.quitsync.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quitsync.viewmodel.JournalViewModel

@Composable
fun JournalScreen(viewModel: JournalViewModel = viewModel()) {
    var journalText by remember { mutableStateOf("") }
    var didSmoke by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "How are you feeling today?", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = journalText,
            onValueChange = { journalText = it },
            modifier = Modifier.fillMaxWidth().height(200.dp),
            placeholder = { Text("Write your thoughts here...") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = didSmoke,
                onCheckedChange = { didSmoke = it }
            )
            Text(text = "I smoked today")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (journalText.isNotBlank()) {
                    viewModel.saveJournalEntry(journalText, didSmoke)
                    journalText = ""
                    didSmoke = false
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Journal")
        }
    }
}
