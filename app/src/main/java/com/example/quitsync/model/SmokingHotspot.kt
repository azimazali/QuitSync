package com.example.quitsync.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class SmokingHotspot(
    @DocumentId val id: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val smoker_user_ids: List<String> = emptyList(),
    val unique_smokers_count: Int = 0,
    @ServerTimestamp val lastUpdated: Date? = null
)
