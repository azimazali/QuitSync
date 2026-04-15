package com.example.quitsync.service

import android.content.Context
import android.util.Log
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.language.v1.Document
import com.google.cloud.language.v1.LanguageServiceClient
import com.google.cloud.language.v1.LanguageServiceSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SentimentAnalyzer(private val context: Context) {

    private var languageClient: LanguageServiceClient? = null

    // Strictly internal and must be called from Dispatchers.IO
    private fun getLanguageClient(): LanguageServiceClient? {
        if (languageClient != null) return languageClient

        return try {
            context.assets.open("google-cloud-key.json").use { inputStream ->
                val credentials = GoogleCredentials.fromStream(inputStream)
                val settings = LanguageServiceSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build()
                languageClient = LanguageServiceClient.create(settings)
                languageClient
            }
        } catch (e: Exception) {
            Log.e("SentimentAnalyzer", "Lazy initialization failed: ${e.message}", e)
            null
        }
    }

    suspend fun analyzeSentiment(text: String): String = withContext(Dispatchers.IO) {
        // Heavy initialization happens here, safely on the IO thread, ONLY when needed.
        val client = getLanguageClient() ?: return@withContext "Neutral"

        return@withContext try {
            val doc = Document.newBuilder()
                .setContent(text)
                .setType(Document.Type.PLAIN_TEXT)
                .build()

            val response = client.analyzeSentiment(doc)
            val sentiment = response.documentSentiment
            val score = sentiment.score

            when {
                score > 0.25 -> "Positive"
                score < -0.25 -> "Negative"
                else -> "Neutral"
            }
        } catch (e: Exception) {
            Log.e("SentimentAnalyzer", "Analysis error: ${e.message}", e)
            "Neutral"
        }
    }
}
