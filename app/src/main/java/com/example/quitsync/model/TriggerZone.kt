package com.example.quitsync.model

import com.google.firebase.firestore.DocumentId

data class TriggerZone(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radius: Float = 100f // Default 100 meters
)
