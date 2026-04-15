package com.example.quitsync.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class JournalEntry(
    val userId: String = "",
    val content: String = "",
    val sentiment: String = "Neutral",
    val didSmoke: Boolean = false,
    @ServerTimestamp val timestamp: Date? = null
)
