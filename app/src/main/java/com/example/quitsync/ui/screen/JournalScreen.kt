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
import com.example.quitsync.ui.components.showcaseTarget
import com.example.quitsync.viewmodel.AuthViewModel

import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.android.gms.location.LocationServices
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

@Composable
fun JournalScreen(
    viewModel: JournalViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
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
                .padding(bottom = 16.dp, end = 16.dp)
                .showcaseTarget("journal_add") { tag, rect ->
                    authViewModel.updateShowcaseTarget(tag, rect)
                },
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Entry")
        }

        if (showAddDialog) {
            JournalEntryDialog(
                title = "New Journal Entry",
                onDismiss = { showAddDialog = false },
                onSave = { text, smoked, lat, lng ->
                    viewModel.saveJournalEntry(text, smoked, lat, lng)
                    showAddDialog = false
                }
            )
        }

        if (entryToEdit != null) {
            JournalEntryDialog(
                title = "Edit Entry",
                initialText = entryToEdit!!.content,
                initialSmoked = entryToEdit!!.didSmoke,
                initialLat = entryToEdit!!.latitude,
                initialLng = entryToEdit!!.longitude,
                onDismiss = { entryToEdit = null },
                onSave = { text, smoked, lat, lng ->
                    viewModel.updateJournalEntry(entryToEdit!!.id, text, smoked, lat, lng)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⚠️ Smoked here", style = MaterialTheme.typography.labelSmall, color = Color.Red)
                    if (entry.latitude != null && entry.longitude != null) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Location", modifier = Modifier.size(12.dp), tint = Color.Red)
                    }
                }
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
    initialLat: Double? = null,
    initialLng: Double? = null,
    onDismiss: () -> Unit,
    onSave: (String, Boolean, Double?, Double?) -> Unit
) {
    var journalText by remember { mutableStateOf(initialText) }
    var didSmoke by remember { mutableStateOf(initialSmoked) }
    var selectedLocation by remember { mutableStateOf(if (initialLat != null && initialLng != null) LatLng(initialLat, initialLng) else null) }
    var showMapPicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(didSmoke) {
        if (didSmoke && (selectedLocation == null || initialLat == null)) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        selectedLocation = LatLng(location.latitude, location.longitude)
                    }
                }
            }
        }
    }

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
                
                if (didSmoke) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showMapPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PinDrop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedLocation != null) "Location Pinned" else "Pin Smoking Location")
                    }
                    if (selectedLocation != null) {
                        Text(
                            text = "Pinned: ${String.format("%.4f", selectedLocation!!.latitude)}, ${String.format("%.4f", selectedLocation!!.longitude)}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { 
                            if (journalText.isNotBlank()) {
                                onSave(journalText, didSmoke, selectedLocation?.latitude, selectedLocation?.longitude)
                            }
                        },
                        enabled = journalText.isNotBlank()
                    ) { Text("Save") }
                }
            }
        }
    }

    if (showMapPicker) {
        LocationPickerDialog(
            initialLocation = selectedLocation,
            onDismiss = { showMapPicker = false },
            onLocationSelected = {
                selectedLocation = it
                showMapPicker = false
            }
        )
    }
}

@Composable
fun LocationPickerDialog(
    initialLocation: LatLng?,
    onDismiss: () -> Unit,
    onLocationSelected: (LatLng) -> Unit
) {
    var tempLocation by remember { mutableStateOf(initialLocation ?: LatLng(1.3521, 103.8198)) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(tempLocation, 12f)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().height(500.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column {
                Box(modifier = Modifier.weight(1f)) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        onMapClick = { tempLocation = it }
                    ) {
                        Marker(
                            state = MarkerState(position = tempLocation),
                            title = "Smoking Place"
                        )
                    }
                    Text(
                        "Tap to pin where you smoked",
                        modifier = Modifier.align(Alignment.TopCenter).padding(8.dp).padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onLocationSelected(tempLocation) }) {
                        Text("Confirm Pin")
                    }
                }
            }
        }
    }
}
