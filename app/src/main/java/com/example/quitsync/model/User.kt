package com.example.quitsync.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val role: String = "user",
    val cigarettePrice: Double = 0.0, // Price per pack (e.g., RM 17.50)
    val dailyCigarettes: Int = 20,    // Average smoked per day
    val cigarettesPerPack: Int = 20,  // Number of sticks in a pack
    @ServerTimestamp val quitDate: Date? = null,
    val nicotineDependenceScore: Int = 0,
    val nicotineDependenceCategory: String = "",
    val goals: List<String> = emptyList(),
    val hasCompletedSetup: Boolean = false,
    val hasSeenTour: Boolean = false
)
