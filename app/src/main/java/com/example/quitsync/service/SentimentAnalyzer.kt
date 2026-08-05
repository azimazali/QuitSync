package com.example.quitsync.service

import android.content.Context
import android.util.Log
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class SentimentAnalyzer(private val context: Context) {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        try {
            context.assets.open("google-cloud-key.json").use { inputStream ->
                val credentials = GoogleCredentials.fromStream(inputStream)
                    .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))
                credentials.refreshIfExpired()
                credentials.accessToken.tokenValue
            }
        } catch (e: Exception) {
            Log.e("SentimentAnalyzer", "Failed to get access token: ${e.message}", e)
            null
        }
    }

    suspend fun analyzeSentiment(text: String): String = withContext(Dispatchers.IO) {
        try {
            val token = getAccessToken()
            if (token == null) {
                Log.d("SentimentAnalyzer", "No access token. Falling back to local sentiment analysis.")
                val localResult = analyzeSentimentLocally(text)
                Log.d("SentimentAnalyzer", "Local analysis result for '$text': $localResult")
                return@withContext localResult
            }
            
            Log.d("SentimentAnalyzer", "Access token retrieved successfully. Calling GCP Natural Language API...")
            val url = "https://language.googleapis.com/v1/documents:analyzeSentiment"

            val jsonBody = JSONObject().apply {
                put("document", JSONObject().apply {
                    put("type", "PLAIN_TEXT")
                    put("content", text)
                })
                put("encodingType", "UTF8")
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .post(jsonBody.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e("SentimentAnalyzer", "API call failed with code ${response.code}. Error body: $errorBody")
                    val fallback = analyzeSentimentLocally(text)
                    Log.d("SentimentAnalyzer", "Local analysis fallback result for '$text': $fallback")
                    return@withContext fallback
                }

                val responseBody = response.body?.string() ?: run {
                    val fallback = analyzeSentimentLocally(text)
                    Log.d("SentimentAnalyzer", "Empty response body. Local fallback result: $fallback")
                    return@withContext fallback
                }
                
                val score = JSONObject(responseBody)
                    .getJSONObject("documentSentiment")
                    .getDouble("score")

                val adjustedScore = adjustScoreForCessationDomain(text, score)

                val result = when {
                    adjustedScore > 0.25 -> "Positive"
                    adjustedScore < -0.25 -> "Negative"
                    else -> "Neutral"
                }
                
                Log.d("SentimentAnalyzer", "GCP analysis success for '$text'. Original Score: $score, Adjusted Score: $adjustedScore, Result: $result")
                result
            }
        } catch (e: Exception) {
            Log.e("SentimentAnalyzer", "Analysis error: ${e.message}. Falling back to local.", e)
            val fallback = analyzeSentimentLocally(text)
            Log.d("SentimentAnalyzer", "Local analysis fallback result for '$text': $fallback")
            fallback
        }
    }

    private fun adjustScoreForCessationDomain(text: String, score: Double): Double {
        val normalized = text.lowercase()
        var adjusted = score
        
        // Positive indicators in cessation context
        val strongPositiveIndicators = listOf(
            "not smoking", "no smoking", "stopped smoking", "quit smoking",
            "smoke free", "smokefree", "didn't smoke", "did not smoke",
            "avoided smoking", "without smoking", "without a cigarette",
            "no cigarette", "no cigarettes", "no craving", "no cravings",
            "resisted", "abstaining", "stayed smoke free"
        )
        
        for (phrase in strongPositiveIndicators) {
            if (normalized.contains(phrase)) {
                adjusted += 0.8
            }
        }
        
        return adjusted.coerceIn(-1.0, 1.0)
    }

    fun analyzeSentimentLocally(text: String): String {
        if (text.isBlank()) return "Neutral"
        
        val words = text.lowercase()
            .replace(Regex("[^a-zA-Z\\s]"), "")
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }

        val positiveWords = setOf(
            "proud", "happy", "good", "great", "excellent", "healthy", "strong", 
            "easy", "fresh", "clean", "quit", "success", "stopped", "improve", 
            "motivation", "motivated", "better", "accomplished", "control", 
            "saving", "free", "confident", "achieved", "calm", "energetic", "glad",
            "wonderful", "fantastic", "amazing", "productive"
        )

        val negativeWords = setOf(
            "stress", "stressed", "craving", "cravings", "sad", "bad", "hard", 
            "difficult", "anxious", "anxiety", "depressed", "relapse", "smoke", 
            "smoked", "smoking", "smoker", "smokes", "fail", "failed", "struggle", 
            "struggling", "angry", "tired", "urge", "temptation", "lonely", "weak", 
            "frustrated", "bored", "pain", "hurt", "exhausted", "hopeless"
        )

        val negations = setOf("no", "not", "never", "dont", "cant", "without", "havent")

        var posCount = 0
        var negCount = 0
        
        var i = 0
        while (i < words.size) {
            val word = words[i]
            
            if (positiveWords.contains(word)) {
                // Check if preceded by negation within 2 words
                val isNegated = (i > 0 && negations.contains(words[i - 1])) || 
                                (i > 1 && negations.contains(words[i - 2]))
                if (isNegated) negCount++ else posCount++
            } else if (negativeWords.contains(word)) {
                // Check if preceded by negation within 2 words
                val isNegated = (i > 0 && negations.contains(words[i - 1])) || 
                                (i > 1 && negations.contains(words[i - 2]))
                if (isNegated) posCount++ else negCount++
            }
            i++
        }

        val total = posCount + negCount
        if (total == 0) return "Neutral"

        val score = (posCount - negCount).toDouble() / total.toDouble()
        return when {
            score > 0.15 -> "Positive"
            score < -0.15 -> "Negative"
            else -> "Neutral"
        }
    }
}
