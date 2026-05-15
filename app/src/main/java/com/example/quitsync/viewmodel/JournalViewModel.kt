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

    fun saveJournalEntry(text: String, didSmoke: Boolean) {
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
                    timestamp = Date()
                )

                db.collection("journals").add(entry)
                    .addOnSuccessListener {
                        if (didSmoke) {
                            db.collection("users").document(currentUserId).update("quitDate", Date())
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

    fun updateJournalEntry(entryId: String, text: String, didSmoke: Boolean) {
        if (entryId.isEmpty()) {
            Log.e("JournalViewModel", "Update failed: entryId is empty")
            return
        }
        val currentUserId = auth.currentUser?.uid ?: return
        _uiState.value = JournalUiState.Loading

        viewModelScope.launch {
            try {
                val sentimentResult = sentimentAnalyzer.analyzeSentiment(text)
                db.collection("journals").document(entryId)
                    .update(mapOf("content" to text, "didSmoke" to didSmoke, "sentiment" to sentimentResult))
                    .addOnSuccessListener {
                        if (didSmoke) {
                            db.collection("users").document(currentUserId).update("quitDate", Date())
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
