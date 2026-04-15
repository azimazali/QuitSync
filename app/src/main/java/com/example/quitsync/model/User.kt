package com.example.quitsync.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    val uid: String = "",
    val email: String = "",
    @ServerTimestamp val quitDate: Date? = null
)
