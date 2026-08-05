package com.example.quitsync.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.quitsync.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions

class AdminViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _users = mutableStateOf<List<User>>(emptyList())
    val users: State<List<User>> = _users

    init {
        fetchAllUsers()
    }

    private fun fetchAllUsers() {
        db.collection("users")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val userList = snapshot?.toObjects(User::class.java) ?: emptyList()

                val groupedByEmail = userList.groupBy { it.email.lowercase().trim() }
                val activeUsers = mutableListOf<User>()
                val staleUserIdsToDelete = mutableListOf<String>()

                for ((email, usersWithEmail) in groupedByEmail) {
                    if (email.isEmpty()) {
                        activeUsers.addAll(usersWithEmail)
                        continue
                    }

                    if (usersWithEmail.size > 1) {
                        // Sort descending by quitDate (nulls last)
                        val sortedUsers = usersWithEmail.sortedWith { u1, u2 ->
                            val d1 = u1.quitDate
                            val d2 = u2.quitDate
                            when {
                                d1 == null && d2 == null -> 0
                                d1 == null -> 1
                                d2 == null -> -1
                                else -> d2.compareTo(d1)
                            }
                        }
                        activeUsers.add(sortedUsers.first())
                        sortedUsers.drop(1).forEach { staleUser ->
                            staleUserIdsToDelete.add(staleUser.uid)
                        }
                    } else {
                        activeUsers.add(usersWithEmail.first())
                    }
                }

                if (staleUserIdsToDelete.isNotEmpty()) {
                    staleUserIdsToDelete.forEach { userId ->
                        db.collection("users").document(userId).delete()
                            .addOnSuccessListener {
                                Log.d("AdminViewModel", "Successfully deleted stale duplicate user doc: $userId")
                            }
                            .addOnFailureListener { err ->
                                Log.e("AdminViewModel", "Failed to delete stale duplicate user doc: ${err.message}")
                            }
                    }
                }

                _users.value = activeUsers
            }
    }

    fun deleteUser(userId: String) {
        Log.d("AdminViewModel", "Starting admin cascading delete for user: $userId")

        val functions = FirebaseFunctions.getInstance("asia-southeast1")
        functions.getHttpsCallable("adminDeleteUserAuth")
            .call(mapOf("userId" to userId))
            .addOnSuccessListener {
                Log.d("AdminViewModel", "Successfully deleted user credentials from Auth. Proceeding with database cleanup...")

                val journalsTask = db.collection("journals").whereEqualTo("userId", userId).get()
                    .continueWithTask { task ->
                        val deletes = task.result?.documents?.map { it.reference.delete() } ?: emptyList()
                        Tasks.whenAll(deletes)
                    }

                val triggerZonesTask = db.collection("trigger_zones").whereEqualTo("userId", userId).get()
                    .continueWithTask { task ->
                        val deletes = task.result?.documents?.map { it.reference.delete() } ?: emptyList()
                        Tasks.whenAll(deletes)
                    }

                val postsTask = db.collection("forum_posts").whereEqualTo("userId", userId).get()
                    .continueWithTask { task ->
                        val deletes = task.result?.documents?.map { it.reference.delete() } ?: emptyList()
                        Tasks.whenAll(deletes)
                    }

                val commentsTask = db.collectionGroup("comments").whereEqualTo("userId", userId).get()
                    .continueWithTask { task ->
                        val deletes = task.result?.documents?.map { it.reference.delete() } ?: emptyList()
                        Tasks.whenAll(deletes)
                    }

                val likesTask = db.collection("forum_posts").whereArrayContains("likedBy", userId).get()
                    .continueWithTask { task ->
                        val updates = task.result?.documents?.map { 
                            it.reference.update("likedBy", FieldValue.arrayRemove(userId))
                        } ?: emptyList()
                        Tasks.whenAll(updates)
                    }

                Tasks.whenAll(journalsTask, triggerZonesTask, postsTask, commentsTask, likesTask)
                    .addOnSuccessListener {
                        db.collection("users").document(userId).delete()
                            .addOnSuccessListener {
                                Log.d("AdminViewModel", "Successfully deleted user profile and all database collections for UID: $userId")
                            }
                            .addOnFailureListener { e ->
                                Log.e("AdminViewModel", "Failed to delete user profile document: ${e.message}")
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("AdminViewModel", "Failed to delete user's subcollections: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("AdminViewModel", "Failed to delete user credentials from Auth: ${e.message}")
            }
    }

    fun updateUserRole(userId: String, newRole: String) {
        db.collection("users").document(userId).update("role", newRole)
    }
}
