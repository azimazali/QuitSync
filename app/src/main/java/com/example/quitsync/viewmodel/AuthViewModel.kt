package com.example.quitsync.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import com.example.quitsync.model.User
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    //State to track if the user is currently logged in
    private val _isUserLoggedIn = mutableStateOf(auth.currentUser != null)
    val isUserLoggedIn: State<Boolean> = _isUserLoggedIn

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
                            quitDate = Date() // Default quit date to signup time
                        )
                        db.collection("users").document(firebaseUser.uid).set(newUser)
                            .addOnSuccessListener {
                                _isUserLoggedIn.value = true
                                onResult(true, null)
                            }
                            .addOnFailureListener { e ->
                                onResult(false, "Failed to create user profile: ${e.message}")
                            }
                    } else {
                        onResult(false, "User creation failed.")
                    }
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    fun signIn(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _isUserLoggedIn.value = true
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }
}
