package com.example.quitsync.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quitsync.model.JournalEntry
import com.example.quitsync.service.SentimentAnalyzer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import java.util.Date

sealed class JournalUiState {
    object Idle : JournalUiState()
    object Loading : JournalUiState()
    object Success : JournalUiState()
    data class Error(val message: String) : JournalUiState()
}

class JournalViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val sentimentAnalyzer = SentimentAnalyzer(application)

    private val _uiState = mutableStateOf<JournalUiState>(JournalUiState.Idle)
    val uiState: State<JournalUiState> = _uiState

    private val _entries = mutableStateOf<List<JournalEntry>>(emptyList())
    val entries: State<List<JournalEntry>> = _entries

    init {
        fetchJournalEntries()
        syncSmokingHotspots()
    }

    /**
     * One-time sync to ensure existing private smoking reports are mirrored 
     * to the public hotspot collection for the shared collaborative calculation.
     */
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

    private fun recordSmokingHotspot(latitude: Double, longitude: Double) {
        val currentUserId = auth.currentUser?.uid ?: return
        
        // Round to 3 decimal places for ~110m grid
        val latRounded = "%.3f".format(java.util.Locale.US, latitude)
        val lngRounded = "%.3f".format(java.util.Locale.US, longitude)
        val docId = "${latRounded}_${lngRounded}".replace(".", "_")
        
        val hotspotRef = db.collection("smoking_hotspots").document(docId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(hotspotRef)
            
            if (!snapshot.exists()) {
                val newHotspot = mapOf(
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "smoker_user_ids" to listOf(currentUserId),
                    "unique_smokers_count" to 1,
                    "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                transaction.set(hotspotRef, newHotspot)
            } else {
                val smokerIds = snapshot.get("smoker_user_ids") as? List<String> ?: emptyList()
                if (!smokerIds.contains(currentUserId)) {
                    val updatedIds = smokerIds + currentUserId
                    transaction.update(hotspotRef, "smoker_user_ids", updatedIds)
                    transaction.update(hotspotRef, "unique_smokers_count", com.google.firebase.firestore.FieldValue.increment(1))
                    transaction.update(hotspotRef, "lastUpdated", com.google.firebase.firestore.FieldValue.serverTimestamp())
                }
            }
        }.addOnSuccessListener {
            Log.d("JournalViewModel", "Hotspot aggregated successfully at $docId")
        }.addOnFailureListener { e ->
            Log.e("JournalViewModel", "Hotspot aggregation failed", e)
        }
    }

    fun fetchJournalEntries() {
        val currentUserId = auth.currentUser?.uid ?: run {
            Log.e("JournalViewModel", "Cannot fetch: No user logged in")
            return
        }

        db.collection("journals")
            .whereEqualTo("userId", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("JournalViewModel", "Firestore Error: ${e.message}")
                    fetchWithoutOrdering(currentUserId)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    _entries.value = snapshot.toObjects(JournalEntry::class.java)
                }
            }
    }

    private fun fetchWithoutOrdering(userId: String) {
        db.collection("journals")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                _entries.value = snapshot.toObjects(JournalEntry::class.java)
                    .sortedByDescending { it.timestamp }
            }
    }

    fun saveJournalEntry(text: String, didSmoke: Boolean, lat: Double? = null, lng: Double? = null) {
        val currentUserId = auth.currentUser?.uid ?: return
        _uiState.value = JournalUiState.Loading

        viewModelScope.launch {
            try {
                val sentimentResult = sentimentAnalyzer.analyzeSentiment(text)
                val entry = JournalEntry(
                    userId = currentUserId,
                    content = text,
                    sentiment = sentimentResult,
                    didSmoke = didSmoke,
                    latitude = if (didSmoke) lat else null,
                    longitude = if (didSmoke) lng else null,
                    timestamp = Date()
                )

                db.collection("journals").add(entry)
                    .addOnSuccessListener { docRef ->
                        if (didSmoke && lat != null && lng != null) {
                            db.collection("users").document(currentUserId).update("quitDate", Date())
                            
                            // Collaborative hotspot update
                            recordSmokingHotspot(lat, lng)
                        }
                        _uiState.value = JournalUiState.Success
                    }
                    .addOnFailureListener { e ->
                        Log.e("JournalViewModel", "Save failed: ${e.message}")
                        _uiState.value = JournalUiState.Error(e.localizedMessage ?: "Save failed")
                    }
            } catch (e: Exception) {
                _uiState.value = JournalUiState.Error("Failed to save entry")
            }
        }
    }

    fun updateJournalEntry(entryId: String, text: String, didSmoke: Boolean, lat: Double? = null, lng: Double? = null) {
        if (entryId.isEmpty()) {
            Log.e("JournalViewModel", "Update failed: entryId is empty")
            return
        }
        val currentUserId = auth.currentUser?.uid ?: return
        _uiState.value = JournalUiState.Loading

        viewModelScope.launch {
            try {
                val sentimentResult = sentimentAnalyzer.analyzeSentiment(text)
                val updates = mutableMapOf<String, Any>(
                    "content" to text,
                    "didSmoke" to didSmoke,
                    "sentiment" to sentimentResult
                )
                if (didSmoke) {
                    lat?.let { updates["latitude"] = it }
                    lng?.let { updates["longitude"] = it }
                } else {
                    updates["latitude"] = com.google.firebase.firestore.FieldValue.delete()
                    updates["longitude"] = com.google.firebase.firestore.FieldValue.delete()
                }

                db.collection("journals").document(entryId)
                    .update(updates)
                    .addOnSuccessListener {
                        if (didSmoke && lat != null && lng != null) {
                            db.collection("users").document(currentUserId).update("quitDate", Date())
                            
                             // Collaborative hotspot update
                            recordSmokingHotspot(lat, lng)
                        }
                        _uiState.value = JournalUiState.Success
                    }
                    .addOnFailureListener { e ->
                        Log.e("JournalViewModel", "Update failed: ${e.message}")
                        _uiState.value = JournalUiState.Error("Update failed: ${e.localizedMessage}")
                    }
            } catch (e: Exception) {
                _uiState.value = JournalUiState.Error("Failed to update entry")
            }
        }
    }

    fun deleteJournalEntry(entryId: String) {
        if (entryId.isEmpty()) {
            Log.e("JournalViewModel", "Delete failed: entryId is empty")
            return
        }
        db.collection("journals").document(entryId).delete()
            .addOnFailureListener { e ->
                Log.e("JournalViewModel", "Delete failed: ${e.message}")
                _uiState.value = JournalUiState.Error("Delete failed: ${e.localizedMessage}")
            }
    }

    fun resetState() { _uiState.value = JournalUiState.Idle }
}
