package com.example.quitsync.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.example.quitsync.GeofenceManager
import com.example.quitsync.model.TriggerZone
import com.example.quitsync.model.SmokingHotspot
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.*

sealed class TriggerUiState {
    object Idle : TriggerUiState()
    object Loading : TriggerUiState()
    object Success : TriggerUiState()
    data class Error(val message: String) : TriggerUiState()
}

class TriggerViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val geofenceManager = GeofenceManager(application)

    private val _uiState = mutableStateOf<TriggerUiState>(TriggerUiState.Idle)
    val uiState: State<TriggerUiState> = _uiState

    private val _triggerZones = mutableStateOf<List<TriggerZone>>(emptyList())
    val triggerZones: State<List<TriggerZone>> = _triggerZones

    private val _userCategory = mutableStateOf("")
    val userCategory: State<String> = _userCategory

    private val _desirePercentage = mutableStateOf(0)
    val desirePercentage: State<Int> = _desirePercentage

    private var hotspotListener: ListenerRegistration? = null

    init {
        fetchTriggerZones()
        fetchUserData()
        listenToHotspots()
        syncSmokingHotspots()
    }

    private fun syncSmokingHotspots() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("journals")
            .whereEqualTo("userId", currentUserId)
            .whereEqualTo("didSmoke", true)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && !snapshot.isEmpty) {
                    snapshot.documents.forEach { doc ->
                        val lat = doc.getDouble("latitude")
                        val lng = doc.getDouble("longitude")
                        if (lat != null && lng != null) {
                            recordSmokingHotspot(lat, lng)
                        }
                    }
                }
            }
    }

    private fun listenToHotspots() {
        hotspotListener?.remove()
        hotspotListener = db.collection("smoking_hotspots").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("TriggerViewModel", "Hotspot listener failed: ${e.message}")
                return@addSnapshotListener
            }
            if (snapshot != null) {
                try {
                    val hotspots = snapshot.toObjects(SmokingHotspot::class.java)
                    recalculateRiskForZones(_triggerZones.value, hotspots)
                } catch (ex: Exception) {
                    Log.e("TriggerViewModel", "Error parsing hotspots", ex)
                }
            }
        }
    }

    private fun fetchUserData() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                _userCategory.value = snapshot.getString("nicotineDependenceCategory") ?: ""
                _desirePercentage.value = snapshot.getLong("desirePercentage")?.toInt() ?: 0
            }
        }
    }

    private fun fetchTriggerZones() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("trigger_zones")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("TriggerViewModel", "Trigger zones listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val zones = snapshot.toObjects(TriggerZone::class.java)
                    db.collection("smoking_hotspots").get()
                        .addOnSuccessListener { hotspotSnapshot ->
                            try {
                                recalculateRiskForZones(zones, hotspotSnapshot.toObjects(SmokingHotspot::class.java))
                            } catch (ex: Exception) {
                                Log.e("TriggerViewModel", "Error parsing hotspots in fetch", ex)
                                _triggerZones.value = zones
                            }
                        }
                        .addOnFailureListener {
                            _triggerZones.value = zones
                        }
                }
            }
    }

    private fun recalculateRiskForZones(zones: List<TriggerZone>, hotspots: List<SmokingHotspot>) {
        if (zones.isEmpty()) {
            _triggerZones.value = zones
            return
        }

        val updatedZones = zones.map { zone ->
            val baseRadius = zone.radius

            // Absolute Count Strategy: sum unique smokers within the zone's influence area
            val nearSmokers = hotspots.filter { spot ->
                calculateDistance(zone.latitude, zone.longitude, spot.latitude, spot.longitude) <= (baseRadius * 1.3f)
            }.flatMap { it.smoker_user_ids }.toSet()

            val nearSmokerCount = nearSmokers.size

            val newCategory = when {
                nearSmokerCount >= 3 -> "Red"
                nearSmokerCount >= 2 -> "Yellow"
                else -> "Blue"
            }

            Log.d("TriggerViewModel", "Zone ${zone.name}: $nearSmokerCount unique smokers. Result: $newCategory")

            if (newCategory != zone.category && zone.id.isNotEmpty()) {
                db.collection("trigger_zones").document(zone.id).update("category", newCategory)
            }

            zone.copy(category = newCategory)
        }
        _triggerZones.value = updatedZones
    }

    private fun recordSmokingHotspot(latitude: Double, longitude: Double) {
        val currentUserId = auth.currentUser?.uid ?: return

        // 1. Round to 3 decimal places to create a ~110m grouping grid
        val latRounded = String.format(java.util.Locale.US, "%.3f", latitude)
        val lngRounded = String.format(java.util.Locale.US, "%.3f", longitude)

        // 2. Create the unique collaborative document ID
        val docId = "${latRounded}_${lngRounded}".replace(".", "_")
        val hotspotRef = db.collection("smoking_hotspots").document(docId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(hotspotRef)

            if (!snapshot.exists()) {
                // Initial creation of the collaborative hotspot
                val newHotspot = mapOf(
                    "latitude" to latitude, // Can keep exact lat/lng for mapping
                    "longitude" to longitude,
                    "smoker_user_ids" to listOf(currentUserId),
                    "unique_smokers_count" to 1,
                    "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                transaction.set(hotspotRef, newHotspot)
            } else {
                // Update existing hotspot only if user is new to this spot
                val smokerIds = snapshot.get("smoker_user_ids") as? List<String> ?: emptyList()

                if (!smokerIds.contains(currentUserId)) {
                    val updatedIds = smokerIds + currentUserId
                    transaction.update(hotspotRef, "smoker_user_ids", updatedIds)
                    transaction.update(hotspotRef, "unique_smokers_count", com.google.firebase.firestore.FieldValue.increment(1))
                    transaction.update(hotspotRef, "lastUpdated", com.google.firebase.firestore.FieldValue.serverTimestamp())
                }
            }
        }.addOnSuccessListener {
            Log.d("Hotspot", "Collaborative update successful for grid: $docId")
        }.addOnFailureListener { e ->
            Log.e("Hotspot", "Transaction failed", e)
        }
    }

    fun saveTriggerZone(name: String, lat: Double, lng: Double, radius: Float) {
        val userId = auth.currentUser?.uid ?: run {
            _uiState.value = TriggerUiState.Error("User not logged in")
            return
        }

        _uiState.value = TriggerUiState.Loading

        detectCategory(lat, lng, radius) { category ->
            val zone = TriggerZone(
                userId = userId,
                name = name,
                latitude = lat,
                longitude = lng,
                radius = radius,
                category = category
            )

            db.collection("trigger_zones")
                .add(zone)
                .addOnSuccessListener {
                    try {
                        geofenceManager.addGeofence(zone, _desirePercentage.value)
                        _uiState.value = TriggerUiState.Success
                    } catch (e: Exception) {
                        Log.e("TriggerViewModel", "Geofence error", e)
                        _uiState.value = TriggerUiState.Error("Saved to DB, but failed to activate alert.")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("TriggerViewModel", "Save failed: ${e.message}")
                    _uiState.value = TriggerUiState.Error(e.localizedMessage ?: "Failed to save")
                }
        }
    }

    fun detectCategory(lat: Double, lng: Double, radius: Float, callback: (String) -> Unit) {
        db.collection("smoking_hotspots").get()
            .addOnSuccessListener { snapshot ->
                try {
                    val hotspots = snapshot.toObjects(SmokingHotspot::class.java)
                    
                    val nearSmokers = hotspots.filter { spot ->
                        calculateDistance(lat, lng, spot.latitude, spot.longitude) <= (radius * 1.3f)
                    }.flatMap { it.smoker_user_ids }.toSet()

                    val nearSmokerCount = nearSmokers.size

                    val category = when {
                        nearSmokerCount >= 3 -> "Red"
                        nearSmokerCount >= 2 -> "Yellow"
                        else -> "Blue"
                    }
                    callback(category)
                } catch (e: Exception) {
                    Log.e("TriggerViewModel", "Parsing error in detectCategory", e)
                    callback("Blue")
                }
            }
            .addOnFailureListener { e ->
                Log.e("TriggerViewModel", "Failed to analyze hotspots: ${e.message}")
                callback("Blue")
            }
    }

    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }

    fun updateTriggerZone(zoneId: String, oldName: String, name: String, lat: Double, lng: Double, radius: Float) {
        if (zoneId.isEmpty()) return
        _uiState.value = TriggerUiState.Loading

        detectCategory(lat, lng, radius) { category ->
            val updates = mapOf(
                "name" to name,
                "latitude" to lat,
                "longitude" to lng,
                "radius" to radius,
                "category" to category
            )

            db.collection("trigger_zones").document(zoneId)
                .update(updates)
                .addOnSuccessListener {
                    try {
                        geofenceManager.removeGeofence(oldName)
                        val newZone = TriggerZone(id = zoneId, name = name, latitude = lat, longitude = lng, radius = radius, category = category)
                        geofenceManager.addGeofence(newZone, _desirePercentage.value)
                        _uiState.value = TriggerUiState.Success
                    } catch (e: Exception) {
                        _uiState.value = TriggerUiState.Error("Updated in DB, but failed to update device alert.")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("TriggerViewModel", "Update failed: ${e.message}")
                    _uiState.value = TriggerUiState.Error(e.localizedMessage ?: "Update failed")
                }
        }
    }

    fun deleteTriggerZone(zone: TriggerZone) {
        if (zone.id.isEmpty()) {
            Log.e("TriggerViewModel", "Delete failed: zone ID is empty")
            return
        }

        db.collection("trigger_zones").document(zone.id).delete()
            .addOnSuccessListener {
                geofenceManager.removeGeofence(zone.name)
            }
            .addOnFailureListener { e ->
                Log.e("TriggerViewModel", "Delete failed: ${e.message}")
                _uiState.value = TriggerUiState.Error("Delete failed: ${e.localizedMessage}")
            }
    }

    fun resetState() {
        _uiState.value = TriggerUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        hotspotListener?.remove()
    }
}
