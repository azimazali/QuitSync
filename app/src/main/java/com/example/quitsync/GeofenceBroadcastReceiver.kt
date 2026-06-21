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
                // First, fetch user dependence category to determine the primary message
                FirebaseFirestore.getInstance().collection("users").document(userId).get()
                    .addOnSuccessListener { userDoc ->
                        val userDependence = userDoc.getString("nicotineDependenceCategory") ?: ""
                        val isHighDependence = userDependence.equals("High Dependence", ignoreCase = true)

                        // Database Integration: Save the last_visited_risk_level to the user's document
                        FirebaseFirestore.getInstance().collection("users").document(userId)
                            .update("last_visited_risk_level", category)
                            .addOnCompleteListener {
                                // Risk Assignment & Notification
                                val message = if (isHighDependence) {
                                    "High Risk Area: Stay strong!"
                                } else {
                                    when (category) {
                                        "Red" -> "High-risk social area detected! Take a deep breath."
                                        "Orange" -> "Warning: Increased risk in this area."
                                        else -> "You've entered a trigger zone."
                                    }
                                }
                                sendNotification(context, message)
                                pendingResult.finish()
                            }
                    }
            }        }
    }

    private fun detectSocialRisk(context: Context, callback: (String, Int) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        
        // 1. Get current location from geofencing event is tricky, but we can use the Last Location
        val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
        
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location == null) {
                    callback("Blue", 0)
                    return@addOnSuccessListener
                }

                val lat = location.latitude
                val lng = location.longitude

                // Logic: Categorize based on shared journal data
                db.collection("journals")
                    .whereEqualTo("didSmoke", true)
                    .get()
                    .addOnSuccessListener { smokingJournals ->
                        val allSmokers = smokingJournals.documents.mapNotNull { it.getString("userId") }.toSet()
                        val totalSmokerCount = allSmokers.size

                        if (totalSmokerCount == 0) {
                            callback("Blue", 0)
                            return@addOnSuccessListener
                        }

                        // Count unique users who smoked near this location (within ~100 meters)
                        val nearSmokers = smokingJournals.documents.filter { doc ->
                            val jLat = doc.getDouble("latitude")
                            val jLng = doc.getDouble("longitude")
                            if (jLat != null && jLng != null) {
                                calculateDistance(lat, lng, jLat, jLng) <= 100
                            } else {
                                false
                            }
                        }.mapNotNull { it.getString("userId") }.toSet()
                        
                        val nearSmokerCount = nearSmokers.size
                        val percentage = (nearSmokerCount.toFloat() / totalSmokerCount.toFloat()) * 100

                        val category = when {
                            percentage >= 70 -> "Red"
                            percentage >= 40 -> "Orange"
                            else -> "Blue"
                        }
                        
                        callback(category, nearSmokerCount)
                    }
                    .addOnFailureListener {
                        callback("Blue", 0)
                    }
            }
        } catch (e: SecurityException) {
            callback("Blue", 0)
        }
    }

    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
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
