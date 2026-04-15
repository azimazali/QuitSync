package com.example.quitsync.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.quitsync.model.TriggerZone
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

sealed class TriggerUiState {
    object Idle : TriggerUiState()
    object Loading : TriggerUiState()
    object Success : TriggerUiState()
    data class Error(val message: String) : TriggerUiState()
}

class TriggerViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = mutableStateOf<TriggerUiState>(TriggerUiState.Idle)
    val uiState: State<TriggerUiState> = _uiState

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
                _uiState.value = TriggerUiState.Success
            }
            .addOnFailureListener { e ->
                _uiState.value = TriggerUiState.Error(e.localizedMessage ?: "Failed to save trigger zone")
            }
    }

    fun resetState() {
        _uiState.value = TriggerUiState.Idle
    }
}
