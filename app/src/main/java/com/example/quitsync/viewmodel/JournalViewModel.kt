package com.example.quitsync.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quitsync.model.JournalEntry
import com.example.quitsync.service.SentimentAnalyzer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

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

    fun saveJournalEntry(text: String, didSmoke: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: run {
            _uiState.value = JournalUiState.Error("User not logged in")
            return
        }

        _uiState.value = JournalUiState.Loading

        viewModelScope.launch {
            val sentimentResult = sentimentAnalyzer.analyzeSentiment(text)

            val entry = JournalEntry(
                userId = currentUserId,
                content = text,
                sentiment = sentimentResult,
                didSmoke = didSmoke
            )

            db.collection("journals")
                .add(entry)
                .addOnSuccessListener {
                    _uiState.value = JournalUiState.Success
                }
                .addOnFailureListener { e ->
                    _uiState.value = JournalUiState.Error(e.localizedMessage ?: "Failed to save entry")
                }
        }
    }

    fun resetState() {
        _uiState.value = JournalUiState.Idle
    }
}
