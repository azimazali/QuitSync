package com.example.quitsync.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.example.quitsync.GeofenceManager
import com.example.quitsync.model.TriggerZone
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

    init {
        fetchTriggerZones()
        fetchUserData()
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
                    Log.e("TriggerViewModel", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    _triggerZones.value = snapshot.toObjects(TriggerZone::class.java)
                }
            }
    }

    fun saveTriggerZone(name: String, lat: Double, lng: Double, radius: Float) {
        val userId = auth.currentUser?.uid ?: run {
            _uiState.value = TriggerUiState.Error("User not logged in")
            return
        }

        _uiState.value = TriggerUiState.Loading

        val zone = TriggerZone(
            userId = userId,
            name = name,
            latitude = lat,
            longitude = lng,
            radius = radius
        )

        db.collection("trigger_zones")
            .add(zone)
            .addOnSuccessListener {
                try {
                    geofenceManager.addGeofence(zone, _desirePercentage.value)
                    _uiState.value = TriggerUiState.Success
                } catch (e: Exception) {
                    _uiState.value = TriggerUiState.Error("Saved to DB, but failed to activate alert.")
                }
            }
            .addOnFailureListener { e ->
                Log.e("TriggerViewModel", "Save failed: ${e.message}")
                _uiState.value = TriggerUiState.Error(e.localizedMessage ?: "Failed to save")
            }
    }

    fun updateTriggerZone(zoneId: String, oldName: String, name: String, lat: Double, lng: Double, radius: Float) {
        if (zoneId.isEmpty()) return
        _uiState.value = TriggerUiState.Loading

        val updates = mapOf(
            "name" to name,
            "latitude" to lat,
            "longitude" to lng,
            "radius" to radius
        )

        db.collection("trigger_zones").document(zoneId)
            .update(updates)
            .addOnSuccessListener {
                try {
                    // Remove old geofence and add new one
                    geofenceManager.removeGeofence(oldName)
                    val newZone = TriggerZone(id = zoneId, name = name, latitude = lat, longitude = lng, radius = radius)
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
}
