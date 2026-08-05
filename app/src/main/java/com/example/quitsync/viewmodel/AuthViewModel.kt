package com.example.quitsync.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.State
import com.example.quitsync.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.android.gms.tasks.Tasks
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

    private val _isUserDataLoading = mutableStateOf(auth.currentUser != null)
    val isUserDataLoading: State<Boolean> = _isUserDataLoading

    // Showcase Tour State
    private val _showcaseTargets = mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>()
    val showcaseTargets: Map<String, androidx.compose.ui.geometry.Rect> = _showcaseTargets

    fun updateShowcaseTarget(tag: String, rect: androidx.compose.ui.geometry.Rect) {
        _showcaseTargets[tag] = rect
    }

    init {
        if (auth.currentUser != null) {
            fetchCurrentUserData()
        }
    }

    private fun fetchCurrentUserData() {
        val uid = auth.currentUser?.uid ?: return
        Log.d("AuthViewModel", "Starting listener for UID: $uid")
        _isUserDataLoading.value = true

        userListener?.remove()

        userListener = db.collection("users").document(uid).addSnapshotListener { document, error ->
            if (error != null) {
                Log.e("AuthViewModel", "Listen failed: ${error.message}")
                _isUserDataLoading.value = false
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
            _isUserDataLoading.value = false
        }
    }

    fun signUp(username: String, email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val trimmedUsername = username.trim()
        if (trimmedUsername.isEmpty()) {
            onResult(false, "Username cannot be empty.")
            return
        }
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) {
            onResult(false, "Email address cannot be empty.")
            return
        }

        Log.d("AuthViewModel", "Checking username uniqueness: $trimmedUsername")
        db.collection("users")
            .whereEqualTo("username", trimmedUsername)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    onResult(false, "Username is already taken.")
                } else {
                    Log.d("AuthViewModel", "Attempting signUp for: $trimmedEmail")
                    auth.createUserWithEmailAndPassword(trimmedEmail, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val firebaseUser = auth.currentUser
                                if (firebaseUser != null) {
                                    val newUser = User(
                                        uid = firebaseUser.uid,
                                        username = trimmedUsername,
                                        email = trimmedEmail,
                                        role = "user",
                                        cigarettePrice = 0.0,
                                        dailyCigarettes = 10,
                                        cigarettesPerPack = 20,
                                        quitDate = Date()
                                    )
                                    db.collection("users").document(firebaseUser.uid).set(newUser)
                                        .addOnSuccessListener {
                                            Log.d("AuthViewModel", "User profile created successfully in Firestore")
                                            _isUserLoggedIn.value = true
                                            fetchCurrentUserData()
                                            onResult(true, null)
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("AuthViewModel", "Failed to create Firestore profile", e)
                                            onResult(false, "Failed to create profile: ${e.message}")
                                        }
                                }
                            } else {
                                val exception = task.exception
                                Log.e("AuthViewModel", "signUp failed", exception)
                                onResult(false, exception?.message)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("AuthViewModel", "Failed to check username uniqueness", e)
                onResult(false, "Verification failed: ${e.message}")
            }
    }

    fun signIn(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val trimmedEmail = email.trim()
        Log.d("AuthViewModel", "Attempting signIn for: $trimmedEmail")
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AuthViewModel", "signIn successful")
                    _isUserLoggedIn.value = true
                    fetchCurrentUserData()
                    onResult(true, null)
                } else {
                    val exception = task.exception
                    Log.e("AuthViewModel", "signIn failed", exception)
                    onResult(false, exception?.message)
                }
            }
    }

    fun signInWithGoogle(credential: AuthCredential, onResult: (Boolean, String?) -> Unit) {
        Log.d("AuthViewModel", "Attempting signInWithGoogle")
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        // Check if user already exists in Firestore
                        db.collection("users").document(firebaseUser.uid).get()
                            .addOnSuccessListener { document ->
                                if (!document.exists()) {
                                    // Create new user profile if it doesn't exist
                                    val emailPrefix = firebaseUser.email?.substringBefore("@") ?: "user"
                                    val uniqueGoogleUsername = "${emailPrefix}_${firebaseUser.uid.take(4)}"
                                    val newUser = User(
                                        uid = firebaseUser.uid,
                                        username = uniqueGoogleUsername,
                                        email = firebaseUser.email ?: "",
                                        role = "user",
                                        cigarettePrice = 0.0,
                                        dailyCigarettes = 10,
                                        cigarettesPerPack = 20,
                                        quitDate = Date()
                                    )
                                    db.collection("users").document(firebaseUser.uid).set(newUser)
                                        .addOnSuccessListener {
                                            Log.d("AuthViewModel", "Google user profile created in Firestore")
                                            _isUserLoggedIn.value = true
                                            fetchCurrentUserData()
                                            onResult(true, null)
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("AuthViewModel", "Failed to create Google user profile", e)
                                            onResult(false, "Failed to create profile: ${e.message}")
                                        }
                                } else {
                                    Log.d("AuthViewModel", "Google user already exists in Firestore")
                                    _isUserLoggedIn.value = true
                                    fetchCurrentUserData()
                                    onResult(true, null)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("AuthViewModel", "Failed to check Google user existence", e)
                                onResult(false, "Failed to check profile: ${e.message}")
                            }
                    }
                } else {
                    val exception = task.exception
                    Log.e("AuthViewModel", "signInWithGoogle failed", exception)
                    onResult(false, exception?.message)
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

    fun completeOnboarding(onResult: (Boolean, String?) -> Unit) {
        val uid = auth.currentUser?.uid ?: run {
            onResult(false, "User not authenticated")
            return
        }
        val updates = mapOf(
            "hasCompletedSetup" to true
        )

        db.collection("users").document(uid).set(updates, SetOptions.merge())
            .addOnSuccessListener { onResult(true, "Onboarding completed") }
            .addOnFailureListener { onResult(false, it.localizedMessage) }
    }

    fun completeTour() {
        val uid = auth.currentUser?.uid ?: return
        val updates = mapOf("hasSeenTour" to true)
        db.collection("users").document(uid).set(updates, SetOptions.merge())
            .addOnFailureListener { Log.e("AuthViewModel", "Failed to update tour status: ${it.message}") }
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
        _showcaseTargets.clear()
    }

    fun deleteAccount(onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: run {
            onResult(false, "No user logged in")
            return
        }
        val uid = user.uid

        Log.d("AuthViewModel", "Starting cascading delete for UID: $uid")

        // 1. Define deletion tasks for related collections
        val journalsTask = db.collection("journals").whereEqualTo("userId", uid).get()
            .continueWithTask { task ->
                val deletes = task.result?.documents?.map { it.reference.delete() } ?: emptyList()
                Tasks.whenAll(deletes)
            }

        val triggerZonesTask = db.collection("trigger_zones").whereEqualTo("userId", uid).get()
            .continueWithTask { task ->
                val deletes = task.result?.documents?.map { it.reference.delete() } ?: emptyList()
                Tasks.whenAll(deletes)
            }

        val postsTask = db.collection("forum_posts").whereEqualTo("userId", uid).get()
            .continueWithTask { task ->
                val deletes = task.result?.documents?.map { it.reference.delete() } ?: emptyList()
                Tasks.whenAll(deletes)
            }

        val commentsTask = db.collectionGroup("comments").whereEqualTo("userId", uid).get()
            .continueWithTask { task ->
                val deletes = task.result?.documents?.map { it.reference.delete() } ?: emptyList()
                Tasks.whenAll(deletes)
            }

        val likesTask = db.collection("forum_posts").whereArrayContains("likedBy", uid).get()
            .continueWithTask { task ->
                val updates = task.result?.documents?.map { 
                    it.reference.update("likedBy", FieldValue.arrayRemove(uid))
                } ?: emptyList()
                Tasks.whenAll(updates)
            }

        // 2. Wait for all background data to be deleted
        Tasks.whenAll(journalsTask, triggerZonesTask, postsTask, commentsTask, likesTask)
            .addOnSuccessListener {
                Log.d("AuthViewModel", "Related data deleted, now deleting user document")
                // 3. Delete user document
                db.collection("users").document(uid).delete()
                    .addOnSuccessListener {
                        Log.d("AuthViewModel", "User document deleted, now deleting auth account")
                        // 4. Delete Auth account
                        user.delete()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    _isUserLoggedIn.value = false
                                    _currentUserData.value = null
                                    onResult(true, null)
                                } else {
                                    Log.e("AuthViewModel", "Auth deletion failed", task.exception)
                                    onResult(false, task.exception?.message ?: "Failed to delete account from Auth. You might need to re-authenticate.")
                                }
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("AuthViewModel", "User document deletion failed", e)
                        onResult(false, "Failed to delete profile data: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("AuthViewModel", "Cascading data deletion failed", e)
                onResult(false, "Failed to delete related data: ${e.message}")
            }
    }

    /**
     * Aggregates smoking location data into a collaborative hotspot grid (~110m).
     * Rounds coordinates to 3 decimal places to group users nearby.
     */
    fun recordSmokingHotspot(latitude: Double, longitude: Double) {
        val currentUserId = auth.currentUser?.uid ?: return
        
        // 1. Round to 3 decimal places for ~110m grid
        val latRounded = "%.3f".format(java.util.Locale.US, latitude)
        val lngRounded = "%.3f".format(java.util.Locale.US, longitude)
        
        // 2. Create unique ID: latRounded_lngRounded (replace . with _)
        val docId = "${latRounded}_${lngRounded}".replace(".", "_")
        val hotspotRef = db.collection("smoking_hotspots").document(docId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(hotspotRef)
            
            if (!snapshot.exists()) {
                // Create new hotspot with exact coordinates of the first report in this grid
                val newHotspot = mapOf(
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "smoker_user_ids" to listOf(currentUserId),
                    "unique_smokers_count" to 1,
                    "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                transaction.set(hotspotRef, newHotspot)
            } else {
                // If exists, add user to array if not already there
                val smokerIds = snapshot.get("smoker_user_ids") as? List<String> ?: emptyList()
                if (!smokerIds.contains(currentUserId)) {
                    val updatedIds = smokerIds + currentUserId
                    transaction.update(hotspotRef, "smoker_user_ids", updatedIds)
                    transaction.update(hotspotRef, "unique_smokers_count", com.google.firebase.firestore.FieldValue.increment(1))
                    transaction.update(hotspotRef, "lastUpdated", com.google.firebase.firestore.FieldValue.serverTimestamp())
                }
            }
        }.addOnSuccessListener {
            Log.d("AuthViewModel", "Hotspot aggregated successfully at grid: $docId")
        }.addOnFailureListener { e ->
            Log.e("AuthViewModel", "Hotspot aggregation failed", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
    }
}
