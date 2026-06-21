package com.example.quitsync.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.quitsync.model.JournalEntry
import com.example.quitsync.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.*
import java.util.concurrent.TimeUnit

data class DayStatus(
    val dayOfMonth: Int,
    val status: Status
)

enum class Status {
    SMOKE_FREE, SMOKED, NO_DATA
}

class HomeViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var userListener: ListenerRegistration? = null
    private var journalListener: ListenerRegistration? = null

    private val _streakDays = mutableStateOf<Long>(0)
    val streakDays: State<Long> = _streakDays

    private val _moneySaved = mutableStateOf<Double>(0.0)
    val moneySaved: State<Double> = _moneySaved

    private val _monthlyStatus = mutableStateOf<List<DayStatus>>(emptyList())
    val monthlyStatus: State<List<DayStatus>> = _monthlyStatus

    private val _currentMonthName = mutableStateOf("")
    val currentMonthName: State<String> = _currentMonthName

    init {
        val calendar = Calendar.getInstance()
        _currentMonthName.value = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
        startRealTimeUpdates()
    }

    private fun startRealTimeUpdates() {
        val userId = auth.currentUser?.uid ?: return

        userListener?.remove()
        userListener = db.collection("users").document(userId)
            .addSnapshotListener { document, error ->
                if (error != null || document == null || !document.exists()) return@addSnapshotListener

                val user = document.toObject(User::class.java)
                val quitDate = user?.quitDate
                val pricePerPack = user?.cigarettePrice ?: 0.0
                val dailyCount = user?.dailyCigarettes ?: 20
                val sticksPerPack = user?.cigarettesPerPack ?: 20

                if (quitDate != null) {
                    calculateProgress(quitDate, pricePerPack, dailyCount, sticksPerPack)
                }
            }

        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.time

        journalListener?.remove()
        journalListener = db.collection("journals")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("timestamp", startOfMonth)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    updateMonthlyStatus(snapshot.toObjects(JournalEntry::class.java))
                }
            }
    }

    private fun calculateProgress(quitDate: Date, price: Double, dailyCigarettes: Int, sticksPerPack: Int) {
        val now = Date()

        // 1. Calculate Streak (Difference in calendar days)
        val calNow = Calendar.getInstance()
        val calQuit = Calendar.getInstance().apply { time = quitDate }

        // Reset both to midnight for accurate calendar day counting
        calNow.set(Calendar.HOUR_OF_DAY, 0)
        calNow.set(Calendar.MINUTE, 0)
        calNow.set(Calendar.SECOND, 0)
        calNow.set(Calendar.MILLISECOND, 0)

        calQuit.set(Calendar.HOUR_OF_DAY, 0)
        calQuit.set(Calendar.MINUTE, 0)
        calQuit.set(Calendar.SECOND, 0)
        calQuit.set(Calendar.MILLISECOND, 0)

        val diffMillis = calNow.timeInMillis - calQuit.timeInMillis
        val days = TimeUnit.MILLISECONDS.toDays(diffMillis)
        _streakDays.value = if (days < 0) 0 else days

        // 2. Calculate Money Saved (RM)
        // Formula: Savings = (Total seconds since quit / seconds in day) * daily cost
        val actualDiffMillis = now.time - quitDate.time
        val secondsSinceQuit = actualDiffMillis / 1000.0
        val secondsInDay = 86400.0

        val costPerStick = if (sticksPerPack > 0) price / sticksPerPack else 0.0
        val dailyCost = dailyCigarettes * costPerStick

        val totalSaved = (secondsSinceQuit / secondsInDay) * dailyCost
        _moneySaved.value = if (totalSaved < 0) 0.0 else totalSaved

        Log.d("HomeViewModel", "Calculation: Streak $days days, Saved RM $totalSaved")
    }

    private fun updateMonthlyStatus(entries: List<JournalEntry>) {
        val statusMap = mutableMapOf<Int, Status>()
        val maxDay = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
        
        // Initialize all days to NO_DATA (Grey) by default
        for (i in 1..maxDay) statusMap[i] = Status.NO_DATA

        for (entry in entries) {
            val day = Calendar.getInstance().apply { time = entry.timestamp ?: Date() }.get(Calendar.DAY_OF_MONTH)
            
            when {
                // If any entry on this day indicates smoking, it's definitely SMOKED (Red)
                entry.didSmoke -> {
                    statusMap[day] = Status.SMOKED
                }
                // If it's not already marked as SMOKED, and we have a "no smoke" entry, mark as SMOKE_FREE (Green)
                statusMap[day] != Status.SMOKED -> {
                    statusMap[day] = Status.SMOKE_FREE
                }
            }
        }
        _monthlyStatus.value = statusMap.map { DayStatus(it.key, it.value) }.sortedBy { it.dayOfMonth }
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
        journalListener?.remove()
    }
}
