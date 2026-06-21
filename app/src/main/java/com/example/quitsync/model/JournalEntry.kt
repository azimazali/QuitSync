package com.example.quitsync.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class JournalEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val content: String = "",
    val sentiment: String = "Neutral",
    val didSmoke: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @ServerTimestamp val timestamp: Date? = null
)
