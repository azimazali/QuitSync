package com.example.quitsync.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.quitsync.model.User
import com.google.firebase.firestore.FirebaseFirestore

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
                _users.value = userList
            }
    }

    fun deleteUser(userId: String) {
        // Note: This only deletes the Firestore document.
        // To delete the actual Auth account, you'd typically use a Firebase Cloud Function.
        db.collection("users").document(userId).delete()
    }

    fun updateUserRole(userId: String, newRole: String) {
        db.collection("users").document(userId).update("role", newRole)
    }
}
