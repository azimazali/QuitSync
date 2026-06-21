package com.example.quitsync.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quitsync.model.TriggerZone
import com.example.quitsync.util.RiskUtils
import com.example.quitsync.viewmodel.TriggerUiState
import com.example.quitsync.viewmodel.TriggerViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.example.quitsync.ui.components.showcaseTarget
import com.example.quitsync.viewmodel.AuthViewModel

@Composable
fun TriggerMapScreen(
    viewModel: TriggerViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState
    val triggerZones by viewModel.triggerZones
    val snackbarHostState = remember { SnackbarHostState() }

    var isMapMaximized by remember { mutableStateOf(false) }
    var zoneToEdit by remember { mutableStateOf<TriggerZone?>(null) }

    // Permissions logic
    var hasFineLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasBackgroundLocationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasFineLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: hasFineLocationPermission
    }

    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasBackgroundLocationPermission = isGranted
    }

    val startLocation = LatLng(1.3521, 103.8198)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startLocation, 12f)
    }

    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var zoneName by remember { mutableStateOf("") }
    var zoneRadius by remember { mutableStateOf(100f) }
    var previewCategory by remember { mutableStateOf("Blue") }

    LaunchedEffect(selectedLocation, zoneRadius) {
        selectedLocation?.let { loc ->
            viewModel.detectCategory(loc.latitude, loc.longitude, zoneRadius) { category ->
                previewCategory = category
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is TriggerUiState.Success) {
            snackbarHostState.showSnackbar(if (zoneToEdit != null) "Trigger zone updated!" else "Trigger zone saved!")
            viewModel.resetState()
            selectedLocation = null
            zoneToEdit = null
        } else if (uiState is TriggerUiState.Error) {
            snackbarHostState.showSnackbar((uiState as TriggerUiState.Error).message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedLocation != null) {
                FloatingActionButton(onClick = {
                    if (!hasFineLocationPermission) {
                        launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasBackgroundLocationPermission) {
                        backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    } else {
                        showDialog = true
                    }
                }) {
                    Icon(if (zoneToEdit != null) Icons.Default.Check else Icons.Default.Add, contentDescription = "Save Trigger Zone")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Map Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (isMapMaximized) 1f else 0.4f)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapLongClick = { selectedLocation = it },
                    properties = MapProperties(isMyLocationEnabled = hasFineLocationPermission)
                ) {
                    // Show saved zones as blue/yellow/red circles/markers (exclude the one being edited to avoid confusion)
                    triggerZones.forEach { zone ->
                        if (zone.id != zoneToEdit?.id) {
                            val zoneColor = RiskUtils.getCategoryColor(zone.category)

                            val adjustedRadius = zone.radius * (1.0f + (viewModel.desirePercentage.value / 100.0f))

                            Marker(
                                state = MarkerState(position = LatLng(zone.latitude, zone.longitude)),
                                title = zone.name,
                                icon = BitmapDescriptorFactory.defaultMarker(
                                    when (zone.category) {
                                        "Red" -> BitmapDescriptorFactory.HUE_RED
                                        "Yellow" -> BitmapDescriptorFactory.HUE_YELLOW
                                        else -> BitmapDescriptorFactory.HUE_AZURE
                                    }
                                )
                            )
                            Circle(
                                center = LatLng(zone.latitude, zone.longitude),
                                radius = adjustedRadius.toDouble(),
                                fillColor = zoneColor.copy(alpha = 0.1f),
                                strokeColor = zoneColor.copy(alpha = 0.5f),
                                strokeWidth = 2f
                            )
                        }
                    }

                    selectedLocation?.let {
                        val previewColor = RiskUtils.getCategoryColor(previewCategory)
                        
                        Marker(
                            state = MarkerState(position = it),
                            title = if (zoneToEdit != null) "New Location for ${zoneToEdit!!.name}" else "Selected Location",
                            snippet = "Risk: $previewCategory",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                when (previewCategory) {
                                    "Red" -> BitmapDescriptorFactory.HUE_RED
                                    "Yellow" -> BitmapDescriptorFactory.HUE_YELLOW
                                    else -> BitmapDescriptorFactory.HUE_AZURE
                                }
                            )
                        )
                        Circle(
                            center = it,
                            radius = zoneRadius.toDouble(),
                            fillColor = previewColor.copy(alpha = 0.2f),
                            strokeColor = previewColor,
                            strokeWidth = 2f
                        )
                    }
                }

                // Fullscreen / Minimize Toggle
                IconButton(
                    onClick = { isMapMaximized = !isMapMaximized },
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = if (isMapMaximized) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Toggle Map Size"
                    )
                }

                if (zoneToEdit != null) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text("Editing: ${zoneToEdit!!.name}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = {
                                zoneToEdit = null
                                selectedLocation = null
                            }) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }

            // Saved Zones List (Only visible when map is not maximized)
            AnimatedVisibility(
                visible = !isMapMaximized,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
                modifier = Modifier.weight(0.6f)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        text = "My Trigger Zones",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.showcaseTarget("map_zones") { tag, rect ->
                            authViewModel.updateShowcaseTarget(tag, rect)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (triggerZones.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No zones saved. Long press map to add one.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(triggerZones) { zone ->
                                TriggerZoneItem(
                                    zone = zone,
                                    onEdit = {
                                        zoneToEdit = it
                                        zoneName = it.name
                                        zoneRadius = it.radius
                                        selectedLocation = LatLng(it.latitude, it.longitude)
                                        cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                            LatLng(it.latitude, it.longitude), 15f
                                        )
                                        isMapMaximized = true // Go to map to edit position
                                    },
                                    onDelete = { viewModel.deleteTriggerZone(it) },
                                    onLocate = {
                                        cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                            LatLng(zone.latitude, zone.longitude), 15f
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(if (zoneToEdit != null) "Update Trigger Zone" else "Save Trigger Zone") },
                text = {
                    Column {
                        Text("Name your trigger zone:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = zoneName,
                            onValueChange = { zoneName = it },
                            label = { Text("Zone Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Radius: ${zoneRadius.toInt()} meters")
                        Slider(
                            value = zoneRadius,
                            onValueChange = { zoneRadius = it },
                            valueRange = 50f..300f,
                            steps = 6 // 50, 100, 150... 300
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedLocation?.let {
                                if (zoneToEdit != null) {
                                    viewModel.updateTriggerZone(zoneToEdit!!.id, zoneToEdit!!.name, zoneName, it.latitude, it.longitude, zoneRadius)
                                } else {
                                    viewModel.saveTriggerZone(zoneName, it.latitude, it.longitude, zoneRadius)
                                }
                            }
                            showDialog = false
                            zoneName = ""
                        },
                        enabled = zoneName.isNotBlank()
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun TriggerZoneItem(zone: TriggerZone, onEdit: (TriggerZone) -> Unit, onDelete: (TriggerZone) -> Unit, onLocate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
         Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = zone.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "Radius: ${zone.radius.toInt()}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            IconButton(onClick = onLocate) {
                Icon(Icons.Default.MyLocation, contentDescription = "Locate", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { onEdit(zone) }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = { onDelete(zone) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        }
    }
}
