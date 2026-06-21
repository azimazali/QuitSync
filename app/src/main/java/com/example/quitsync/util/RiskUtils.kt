package com.example.quitsync.util

import androidx.compose.ui.graphics.Color

object RiskUtils {
    /**
     * Returns a color representing the risk level based on category name.
     */
    fun getCategoryColor(category: String): Color {
        return when (category) {
            "Red" -> Color.Red
            "Yellow" -> Color.Yellow
            "Orange" -> Color(0xFFFFA500) // Keep for legacy zones
            else -> Color.Blue
        }
    }
}
