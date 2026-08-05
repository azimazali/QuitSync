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
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", "Error code: ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            val pendingResult = goAsync()
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                pendingResult.finish()
                return
            }

            // Get triggering location or fallback to last location provider
            val location = geofencingEvent.triggeringLocation
            if (location != null) {
                processLocationRisk(context, userId, location.latitude, location.longitude, geofenceTransition, pendingResult)
            } else {
                val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            processLocationRisk(context, userId, lastLoc.latitude, lastLoc.longitude, geofenceTransition, pendingResult)
                        } else {
                            pendingResult.finish()
                        }
                    }.addOnFailureListener {
                        pendingResult.finish()
                    }
                } catch (e: SecurityException) {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun processLocationRisk(
        context: Context,
        userId: String,
        latitude: Double,
        longitude: Double,
        transitionType: Int,
        pendingResult: PendingResult
    ) {
        val db = FirebaseFirestore.getInstance()

        // 1. Discretize coordinates to 3-decimal places for shared ~110m grid
        val latRounded = String.format(java.util.Locale.US, "%.3f", latitude)
        val lngRounded = String.format(java.util.Locale.US, "%.3f", longitude)
        val docId = "${latRounded}_${lngRounded}".replace(".", "_")

        val hotspotRef = db.collection("smoking_hotspots").document(docId)

        // 2. Perform direct look-up query in smoking_hotspots with cache fallback for Doze mode
        hotspotRef.get().addOnSuccessListener { documentSnapshot ->
            handleHotspotEvaluation(context, userId, documentSnapshot.getLong("unique_smokers_count")?.toInt() ?: 0, transitionType, pendingResult)
        }.addOnFailureListener {
            // Attempt to read cached data if network is throttled in Doze mode
            hotspotRef.get(com.google.firebase.firestore.Source.CACHE).addOnSuccessListener { documentSnapshot ->
                handleHotspotEvaluation(context, userId, documentSnapshot.getLong("unique_smokers_count")?.toInt() ?: 0, transitionType, pendingResult)
            }.addOnFailureListener { e ->
                Log.e("GeofenceReceiver", "Failed to fetch hotspot data from server and cache", e)
                pendingResult.finish()
            }
        }
    }

    private fun handleHotspotEvaluation(
        context: Context,
        userId: String,
        uniqueSmokersCount: Int,
        transitionType: Int,
        pendingResult: PendingResult
    ) {
        val db = FirebaseFirestore.getInstance()

        // 3. Risk assignment via the absolute threshold matrix
        val category = when {
            uniqueSmokersCount >= 3 -> "Red"
            uniqueSmokersCount == 2 -> "Yellow"
            else -> "Blue"
        }

        // 4. Save the last_visited_risk_level to user profile
        db.collection("users").document(userId)
            .update("last_visited_risk_level", category)
            .addOnCompleteListener {
                val isDwell = transitionType == Geofence.GEOFENCE_TRANSITION_DWELL

                // 5. Construct notification message based on risk category and transition event
                val message = when (category) {
                    "Red" -> {
                        if (isDwell) {
                            "URGENT: You have lingered in an active smoking trigger zone! Open urge management helper immediately."
                        } else {
                            "High-risk active smoking trigger zone entered! Take a deep breath."
                        }
                    }
                    "Yellow" -> {
                        if (isDwell) {
                            "Warning: Emerging environmental risk. Try some distraction alternatives."
                        } else {
                            "Warning: Moderate environmental smoking risks nearby."
                        }
                    }
                    else -> null // Blue/Baseline State: Silent logging to user history profile
                }

                if (message != null) {
                    sendNotification(context, message)
                } else {
                    Log.d("GeofenceReceiver", "Risk category is Blue ($uniqueSmokersCount smokers). Silent logging completed, no notification sent.")
                }
                pendingResult.finish()
            }
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
