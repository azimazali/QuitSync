package com.example.quitsync.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    var entryToDelete by remember { mutableStateOf<JournalEntry?>(null) }
    var showUrgeHelper by remember { mutableStateOf(false) }
    var isSuggestionDismissed by remember { mutableStateOf(false) }

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
                    val latestEntry = entries.firstOrNull()
                    if (latestEntry?.sentiment == "Negative" && !isSuggestionDismissed) {
                        item {
                            UrgeSuggestionCard(
                                onOpenHelper = { showUrgeHelper = true },
                                onDismiss = { isSuggestionDismissed = true }
                            )
                        }
                    }
                    items(entries) { entry ->
                        JournalEntryItem(
                            entry = entry,
                            onEdit = { entryToEdit = it },
                            onDelete = { entryToDelete = it }
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

        if (entryToDelete != null) {
            AlertDialog(
                onDismissRequest = { entryToDelete = null },
                title = { Text("Delete Journal Entry") },
                text = { Text("Are you sure you want to delete this journal entry? This action cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            entryToDelete?.let {
                                viewModel.deleteJournalEntry(it.id)
                            }
                            entryToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { entryToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showUrgeHelper) {
            UrgeHelperDialog(onDismiss = { showUrgeHelper = false })
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
    val context = LocalContext.current
    val sharedPrefs = remember(context) {
        context.getSharedPreferences("quitsync_journal_drafts", android.content.Context.MODE_PRIVATE)
    }

    val isNewEntry = title == "New Journal Entry"

    val initialTextLoaded = remember {
        if (isNewEntry) {
            sharedPrefs.getString("draft_text", "") ?: ""
        } else {
            initialText
        }
    }
    val initialSmokedLoaded = remember {
        if (isNewEntry) {
            sharedPrefs.getBoolean("draft_did_smoke", initialSmoked)
        } else {
            initialSmoked
        }
    }
    val initialLatLoaded = remember {
        if (isNewEntry) {
            val latStr = sharedPrefs.getString("draft_lat", null)
            latStr?.toDoubleOrNull() ?: initialLat
        } else {
            initialLat
        }
    }
    val initialLngLoaded = remember {
        if (isNewEntry) {
            val lngStr = sharedPrefs.getString("draft_lng", null)
            lngStr?.toDoubleOrNull() ?: initialLng
        } else {
            initialLng
        }
    }

    var journalText by remember { mutableStateOf(initialTextLoaded) }
    var didSmoke by remember { mutableStateOf(initialSmokedLoaded) }
    var selectedLocation by remember { mutableStateOf(if (initialLatLoaded != null && initialLngLoaded != null) LatLng(initialLatLoaded, initialLngLoaded) else null) }
    var showMapPicker by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(didSmoke) {
        if (didSmoke && (selectedLocation == null || initialLatLoaded == null)) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null && selectedLocation == null) {
                        selectedLocation = LatLng(location.latitude, location.longitude)
                    }
                }
            }
        }
    }

    if (isNewEntry) {
        LaunchedEffect(journalText, didSmoke, selectedLocation) {
            val loc = selectedLocation
            sharedPrefs.edit().apply {
                putString("draft_text", journalText)
                putBoolean("draft_did_smoke", didSmoke)
                if (loc != null) {
                    putString("draft_lat", loc.latitude.toString())
                    putString("draft_lng", loc.longitude.toString())
                } else {
                    remove("draft_lat")
                    remove("draft_lng")
                }
                apply()
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
                    TextButton(onClick = {
                        if (isNewEntry) {
                            sharedPrefs.edit().clear().apply()
                        }
                        onDismiss()
                    }) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { 
                            if (journalText.isNotBlank()) {
                                if (isNewEntry) {
                                    sharedPrefs.edit().clear().apply()
                                }
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

@Composable
fun UrgeSuggestionCard(onOpenHelper: () -> Unit, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Need a moment of calm?",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "We noticed some stress in your last journal entry. Take a short pause with our urge-management helper.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onOpenHelper,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.SelfImprovement, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Urge Helper")
            }
        }
    }
}

@Composable
fun UrgeHelperDialog(onDismiss: () -> Unit) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Breathe, 1 = Distractions

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(480.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Urge Helper",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                TabRow(selectedTabIndex = activeTab, modifier = Modifier.fillMaxWidth()) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Breathe") }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Distract Me") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (activeTab == 0) {
                        BreathingGuideSection()
                    } else {
                        DistractionTasksSection()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("I feel better")
                }
            }
        }
    }
}

@Composable
fun BreathingGuideSection() {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    val scale = if (progress < 0.5f) {
        0.8f + (progress * 2f) * 0.4f
    } else {
        1.2f - ((progress - 0.5f) * 2f) * 0.4f
    }

    val phaseText = if (progress < 0.5f) "Inhale..." else "Exhale..."
    val phaseColor = if (progress < 0.5f) Color(0xFF00B4DB) else Color(0xFF0083B0)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(phaseColor, phaseColor.copy(alpha = 0.3f))
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = phaseText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Follow the expanding circle to regulate your heart rate.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun DistractionTasksSection() {
    val tasks = remember {
        listOf(
            "Drink a glass of cold water slowly.",
            "Do 10 deep shoulder rolls or stretch.",
            "Text or call your support buddy.",
            "Wait 5 minutes — the peak of the craving will pass.",
            "Focus on your financial savings progress."
        )
    }
    val checkedStates = remember { mutableStateListOf(*Array(tasks.size) { false }) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Complete these simple tasks to shift your focus:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        tasks.forEachIndexed { index, task ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checkedStates[index],
                    onCheckedChange = { checkedStates[index] = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = task,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (checkedStates[index]) Color.Gray else Color.Unspecified,
                    textDecoration = if (checkedStates[index]) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )
            }
        }
    }
}
