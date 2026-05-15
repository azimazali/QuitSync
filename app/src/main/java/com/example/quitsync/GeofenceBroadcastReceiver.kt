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

        val geofenceTransition = geofenceTransition(geofencingEvent)

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            
            FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val category = document.getString("nicotineDependenceCategory") ?: ""
                    val isHighRisk = category.equals("High Dependence", ignoreCase = true)
                    
                    triggeringGeofences?.forEach { geofence ->
                        val message = if (isHighRisk) {
                            "High Risk Area: Stay strong!"
                        } else {
                            "You've entered a trigger zone: ${geofence.requestId}"
                        }
                        sendNotification(context, message)
                    }
                }
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
