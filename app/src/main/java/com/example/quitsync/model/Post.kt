package com.example.quitsync.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Post(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "Anonymous",
    val title: String = "",
    val description: String = "",
    val likedBy: List<String> = emptyList(),
    @ServerTimestamp val timestamp: Date? = null
)
