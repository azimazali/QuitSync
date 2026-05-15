package com.example.quitsync.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quitsync.model.JournalEntry
import com.example.quitsync.viewmodel.JournalViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun JournalScreen(viewModel: JournalViewModel = viewModel()) {
    val entries by viewModel.entries
    var showAddDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<JournalEntry?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "My Journal",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No entries yet. Start writing!", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(entries) { entry ->
                        JournalEntryItem(
                            entry = entry,
                            onEdit = { entryToEdit = it },
                            onDelete = { viewModel.deleteJournalEntry(it.id) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Entry")
        }

        if (showAddDialog) {
            JournalEntryDialog(
                title = "New Journal Entry",
                onDismiss = { showAddDialog = false },
                onSave = { text, smoked ->
                    viewModel.saveJournalEntry(text, smoked)
                    showAddDialog = false
                }
            )
        }

        if (entryToEdit != null) {
            JournalEntryDialog(
                title = "Edit Entry",
                initialText = entryToEdit!!.content,
                initialSmoked = entryToEdit!!.didSmoke,
                onDismiss = { entryToEdit = null },
                onSave = { text, smoked ->
                    viewModel.updateJournalEntry(entryToEdit!!.id, text, smoked)
                    entryToEdit = null
                }
            )
        }
    }
}

@Composable
fun JournalEntryItem(entry: JournalEntry, onEdit: (JournalEntry) -> Unit, onDelete: (JournalEntry) -> Unit) {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val dateString = entry.timestamp?.let { sdf.format(it) } ?: "Just now"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Row {
                    IconButton(onClick = { onEdit(entry) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { onDelete(entry) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = Color.Red)
                    }
                }
            }

            SentimentBadge(entry.sentiment)
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = entry.content, style = MaterialTheme.typography.bodyLarge)

            if (entry.didSmoke) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "⚠️ Smoked today", style = MaterialTheme.typography.labelSmall, color = Color.Red)
            }
        }
    }
}

@Composable
fun SentimentBadge(sentiment: String) {
    val color = when (sentiment) {
        "Positive" -> Color(0xFF4CAF50)
        "Negative" -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.extraSmall,
        border = androidx.compose.foundation.BorderStroke(1.dp, color),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Text(
            text = sentiment,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 10.sp,
            color = color
        )
    }
}

@Composable
fun JournalEntryDialog(
    title: String,
    initialText: String = "",
    initialSmoked: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (String, Boolean) -> Unit
) {
    var journalText by remember { mutableStateOf(initialText) }
    var didSmoke by remember { mutableStateOf(initialSmoked) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = journalText,
                    onValueChange = { journalText = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    placeholder = { Text("How are you feeling?") },
                    label = { Text("Your thoughts") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = didSmoke, onCheckedChange = { didSmoke = it })
                    Text(text = "I smoked today")
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (journalText.isNotBlank()) onSave(journalText, didSmoke) },
                        enabled = journalText.isNotBlank()
                    ) { Text("Save") }
                }
            }
        }
    }
}
