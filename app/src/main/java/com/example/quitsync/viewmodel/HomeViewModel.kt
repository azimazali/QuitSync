package com.example.quitsync.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.quitsync.model.JournalEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*
import java.util.concurrent.TimeUnit

data class DayStatus(
    val dayOfMonth: Int,
    val status: Status // Green, Red, or Neutral (no entry)
)

enum class Status {
    SMOKE_FREE, SMOKED, NO_DATA
}

class HomeViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _streakDays = mutableStateOf<Long>(0)
    val streakDays: State<Long> = _streakDays

    private val _monthlyStatus = mutableStateOf<List<DayStatus>>(emptyList())
    val monthlyStatus: State<List<DayStatus>> = _monthlyStatus

    private val _currentMonthName = mutableStateOf("")
    val currentMonthName: State<String> = _currentMonthName

    init {
        val calendar = Calendar.getInstance()
        _currentMonthName.value = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
        fetchStreak()
        fetchMonthlyStatus()
    }

    private fun fetchStreak() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val quitDate = document.getTimestamp("quitDate")?.toDate()
                if (quitDate != null) {
                    val diff = Date().time - quitDate.time
                    _streakDays.value = maxOf(0, TimeUnit.MILLISECONDS.toDays(diff))
                }
            }
    }

    private fun fetchMonthlyStatus() {
        val userId = auth.currentUser?.uid ?: return
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = calendar.time

        db.collection("journals")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("timestamp", startOfMonth)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val entries = documents.toObjects(JournalEntry::class.java)
                val statusMap = mutableMapOf<Int, Status>()

                // Default all days to NO_DATA
                val maxDay = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
                for (i in 1..maxDay) {
                    statusMap[i] = Status.NO_DATA
                }

                for (entry in entries) {
                    val entryDate = entry.timestamp ?: continue
                    val cal = Calendar.getInstance()
                    cal.time = entryDate
                    val day = cal.get(Calendar.DAY_OF_MONTH)

                    // If multiple entries for a day, if any entry says SMOKED, the day is SMOKED
                    val currentStatus = statusMap[day]
                    if (entry.didSmoke) {
                        statusMap[day] = Status.SMOKED
                    } else if (currentStatus != Status.SMOKED) {
                        statusMap[day] = Status.SMOKE_FREE
                    }
                }

                _monthlyStatus.value = statusMap.map { DayStatus(it.key, it.value) }.sortedBy { it.dayOfMonth }
            }
    }
}
