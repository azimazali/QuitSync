package com.example.quitsync.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "Anonymous",
    val content: String = "",
    @ServerTimestamp val timestamp: Date? = null
)
