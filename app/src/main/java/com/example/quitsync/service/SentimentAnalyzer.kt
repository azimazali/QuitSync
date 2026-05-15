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
            val token = getAccessToken() ?: return@withContext "Neutral"
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
                    Log.e("SentimentAnalyzer", "API call failed: ${response.code} ${response.message}")
                    return@withContext "Neutral"
                }

                val responseBody = response.body?.string() ?: return@withContext "Neutral"
                val score = JSONObject(responseBody)
                    .getJSONObject("documentSentiment")
                    .getDouble("score")

                when {
                    score > 0.25 -> "Positive"
                    score < -0.25 -> "Negative"
                    else -> "Neutral"
                }
            }
        } catch (e: Exception) {
            Log.e("SentimentAnalyzer", "Analysis error: ${e.message}", e)
            "Neutral"
        }
    }
}
