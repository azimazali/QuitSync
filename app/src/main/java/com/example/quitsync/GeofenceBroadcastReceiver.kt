package com.example.quitsync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.quitsync.util.RiskUtils
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", "Error code: ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofenceTransition(geofencingEvent)

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val pendingResult = goAsync()
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                pendingResult.finish()
                return
            }

            // Detect social risk using the new Places API logic
            detectSocialRisk(context) { category, score ->
                // Database Integration: Save the last_visited_risk_level to the user's document
                FirebaseFirestore.getInstance().collection("users").document(userId)
                    .update("last_visited_risk_level", category)
                    .addOnCompleteListener {
                        // Risk Assignment & Notification
                        val message = when (category) {
                            "Red" -> "High-risk social area detected! Take a deep breath."
                            "Orange" -> "Warning: Increased risk in this area."
                            else -> "You've entered a trigger zone."
                        }
                        sendNotification(context, message)
                        pendingResult.finish()
                    }
            }
        }
    }

    private fun detectSocialRisk(context: Context, callback: (String, Int) -> Unit) {
        if (!Places.isInitialized()) {
            callback("Blue", 0)
            return
        }

        val placesClient = Places.createClient(context)

        // First, find the most likely current place ID
        val findRequest = FindCurrentPlaceRequest.newInstance(listOf(Place.Field.ID))

        try {
            placesClient.findCurrentPlace(findRequest).addOnSuccessListener { response ->
                val placeId = response.placeLikelihoods.firstOrNull()?.place?.id

                if (placeId != null) {
                    // Use fetchPlace() to retrieve displayName, types, and outdoorSeating as requested
                    val fetchRequest = FetchPlaceRequest.newInstance(placeId, listOf(
                        Place.Field.DISPLAY_NAME,
                        Place.Field.TYPES,
                        Place.Field.OUTDOOR_SEATING
                    ))

                    placesClient.fetchPlace(fetchRequest).addOnSuccessListener { fetchResponse ->
                        val place = fetchResponse.place
                        val displayName = place.displayName?.lowercase() ?: ""
                        val types = place.types ?: emptyList()
                        val hasOutdoor = place.outdoorSeating == Place.BooleanPlaceAttributeValue.TRUE

                        var locationRiskScore = 0
                        var category = "Blue"

                        // Mamak Detection: displayName contains 'mamak' OR 'nasi kandar' AND outdoorSeating is true
                        if ((displayName.contains("mamak") || displayName.contains("nasi kandar")) && hasOutdoor) {
                            locationRiskScore = 8
                        }

                        // Risk Assignment
                        if (locationRiskScore >= 7) {
                            category = "Red"
                        } else if (types.contains(Place.Type.CAFE) && !hasOutdoor) {
                            // If place type is 'CAFE' without outdoor seating: Set category to Orange
                            category = "Orange"
                        } else if (types.contains(Place.Type.BAR) || types.contains(Place.Type.NIGHT_CLUB)) {
                            category = "Red"
                        }

                        callback(category, locationRiskScore)
                    }.addOnFailureListener { e ->
                        Log.e("GeofenceReceiver", "FetchPlace failure: ${e.message}")
                        callback("Blue", 0)
                    }
                } else {
                    callback("Blue", 0)
                }
            }.addOnFailureListener { e ->
                Log.e("GeofenceReceiver", "FindCurrentPlace failure: ${e.message}")
                callback("Blue", 0)
            }
        } catch (e: SecurityException) {
            Log.e("GeofenceReceiver", "Location permission missing")
            callback("Blue", 0)
        }
    }

    private fun geofenceTransition(event: GeofencingEvent): Int {
        return event.geofenceTransition
    }

    private fun sendNotification(context: Context, message: String) {
        val channelId = "trigger_zone_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Trigger Zone Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("QuitSync Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: SecurityException) {
            Log.e("GeofenceReceiver", "Permission missing for notification")
        }
    }
}
