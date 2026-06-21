package com.example.quitsync.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun FagerstromQuestion(
    question: String,
    options: List<Pair<String, Int>>,
    selectedScore: Int,
    onScoreSelected: (Int) -> Unit
) {
    Column {
        Text(text = question, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        options.forEach { (text, score) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (selectedScore == score),
                        onClick = { onScoreSelected(score) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (selectedScore == score),
                    onClick = null // Selected by Row onClick
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

@Composable
fun FagerstromTestContent(
    q1Score: Int, onQ1Selected: (Int) -> Unit,
    q2Score: Int, onQ2Selected: (Int) -> Unit,
    q3Score: Int, onQ3Selected: (Int) -> Unit,
    q4Score: Int, onQ4Selected: (Int) -> Unit,
    q5Score: Int, onQ5Selected: (Int) -> Unit,
    q6Score: Int, onQ6Selected: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        FagerstromQuestion(
            question = "1. How soon after you wake up do you smoke your first cigarette?",
            options = listOf(
                "Within 5 minutes" to 3,
                "6-30 minutes" to 2,
                "31-60 minutes" to 1,
                "After 60 minutes" to 0
            ),
            selectedScore = q1Score,
            onScoreSelected = onQ1Selected
        )

        FagerstromQuestion(
            question = "2. Do you find it difficult to refrain from smoking in places where it is forbidden (e.g., in church, at the library, in cinema, etc.)?",
            options = listOf(
                "Yes" to 1,
                "No" to 0
            ),
            selectedScore = q2Score,
            onScoreSelected = onQ2Selected
        )

        FagerstromQuestion(
            question = "3. Which cigarette would you hate most to give up?",
            options = listOf(
                "The first one in the morning" to 1,
                "Any other" to 0
            ),
            selectedScore = q3Score,
            onScoreSelected = onQ3Selected
        )

        FagerstromQuestion(
            question = "4. How many cigarettes per day do you smoke?",
            options = listOf(
                "10 or less" to 0,
                "11-20" to 1,
                "21-30" to 2,
                "31 or more" to 3
            ),
            selectedScore = q4Score,
            onScoreSelected = onQ4Selected
        )

        FagerstromQuestion(
            question = "5. Do you smoke more frequently during the first hours after waking than during the rest of the day?",
            options = listOf(
                "Yes" to 1,
                "No" to 0
            ),
            selectedScore = q5Score,
            onScoreSelected = onQ5Selected
        )

        FagerstromQuestion(
            question = "6. Do you smoke if you are so ill that you are in bed most of the day?",
            options = listOf(
                "Yes" to 1,
                "No" to 0
            ),
            selectedScore = q6Score,
            onScoreSelected = onQ6Selected
        )
    }
}

fun calculateFagerstromResults(scores: List<Int>): Pair<Int, String> {
    val totalScore = scores.filter { it != -1 }.sum()
    val category = when (totalScore) {
        in 0..2 -> "Very Low Dependence"
        in 3..4 -> "Low Dependence"
        5 -> "Medium Dependence"
        in 6..7 -> "High Dependence"
        in 8..10 -> "Very High Dependence"
        else -> ""
    }
    return totalScore to category
}
