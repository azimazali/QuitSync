package com.example.quitsync.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import com.example.quitsync.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.util.Date

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var userListener: ListenerRegistration? = null

    private val _isUserLoggedIn = mutableStateOf(auth.currentUser != null)
    val isUserLoggedIn: State<Boolean> = _isUserLoggedIn

    private val _currentUserRole = mutableStateOf<String?>("user")
    val currentUserRole: State<String?> = _currentUserRole

    private val _currentUserData = mutableStateOf<User?>(null)
    val currentUserData: State<User?> = _currentUserData

    init {
        if (auth.currentUser != null) {
            fetchCurrentUserData()
        }
    }

    private fun fetchCurrentUserData() {
        val uid = auth.currentUser?.uid ?: return
        Log.d("AuthViewModel", "Starting listener for UID: $uid")

        userListener?.remove()

        userListener = db.collection("users").document(uid).addSnapshotListener { document, error ->
            if (error != null) {
                Log.e("AuthViewModel", "Listen failed: ${error.message}")
                return@addSnapshotListener
            }

            if (document != null && document.exists()) {
                val user = document.toObject(User::class.java)
                val role = user?.role?.lowercase() ?: "user"
                Log.d("AuthViewModel", "User data updated: role=$role, email=${user?.email}")

                _currentUserData.value = user
                _currentUserRole.value = role
            } else {
                Log.w("AuthViewModel", "User document does not exist in Firestore for UID: $uid")
                _currentUserRole.value = "user"
            }
        }
    }

    fun signUp(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) {
            onResult(false, "Email address cannot be empty.")
            return
        }

        auth.createUserWithEmailAndPassword(trimmedEmail, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        val newUser = User(
                            uid = firebaseUser.uid,
                            email = trimmedEmail,
                            role = "user",
                            cigarettePrice = 0.0,
                            dailyCigarettes = 10,
                            cigarettesPerPack = 20,
                            quitDate = Date()
                        )
                        db.collection("users").document(firebaseUser.uid).set(newUser)
                            .addOnSuccessListener {
                                _isUserLoggedIn.value = true
                                fetchCurrentUserData()
                                onResult(true, null)
                            }
                            .addOnFailureListener { e ->
                                onResult(false, "Failed to create profile: ${e.message}")
                            }
                    }
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    fun signIn(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _isUserLoggedIn.value = true
                    fetchCurrentUserData()
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    fun resetPassword(email: String, onResult: (Boolean, String?) -> Unit) {
        if (email.isBlank()) {
            onResult(false, "Please enter your email address.")
            return
        }
        auth.sendPasswordResetEmail(email.trim())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, "Password reset email sent.")
                } else {
                    onResult(false, task.exception?.message ?: "Failed to send reset email.")
                }
            }
    }

    fun updateFinancialData(price: Double, dailyCount: Int, perPack: Int, quitDate: Date, onResult: (Boolean, String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: run {
            onResult(false, "User not authenticated")
            return
        }
        val updates = mapOf(
            "cigarettePrice" to price,
            "dailyCigarettes" to dailyCount,
            "cigarettesPerPack" to perPack,
            "quitDate" to quitDate
        )

        db.collection("users").document(uid).set(updates, SetOptions.merge())
            .addOnSuccessListener { onResult(true, "Financial data updated successfully") }
            .addOnFailureListener { onResult(false, it.localizedMessage) }
    }

    fun updateNicotineDependence(score: Int, category: String, onResult: (Boolean, String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: run {
            onResult(false, "User not authenticated")
            return
        }
        val updates = mapOf(
            "nicotineDependenceScore" to score,
            "nicotineDependenceCategory" to category
        )

        db.collection("users").document(uid).set(updates, SetOptions.merge())
            .addOnSuccessListener { onResult(true, "Dependence data updated successfully") }
            .addOnFailureListener { onResult(false, it.localizedMessage) }
    }

    fun updateGoals(goals: List<String>, onResult: (Boolean, String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: run {
            onResult(false, "User not authenticated")
            return
        }
        val updates = mapOf(
            "goals" to goals
        )

        db.collection("users").document(uid).set(updates, SetOptions.merge())
            .addOnSuccessListener { onResult(true, "Goals updated successfully") }
            .addOnFailureListener { onResult(false, it.localizedMessage) }
    }

    fun changePassword(newPassword: String, onResult: (Boolean, String?) -> Unit) {
        auth.currentUser?.updatePassword(newPassword)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, "Password updated successfully")
                } else {
                    onResult(false, task.exception?.message ?: "Failed to update password")
                }
            }
    }

    fun logout() {
        userListener?.remove()
        auth.signOut()
        _isUserLoggedIn.value = false
        _currentUserData.value = null
        _currentUserRole.value = "user"
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
    }
}
