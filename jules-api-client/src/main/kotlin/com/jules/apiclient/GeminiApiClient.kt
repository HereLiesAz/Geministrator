package com.jules.apiclient

import com.google.genai.client.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiApiClient(
    apiKey: String,
    modelName: String = "gemini-pro"
) {

    private val model = GenerativeModel(modelName, apiKey)

    suspend fun generateContent(prompt: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val response = model.generateContent(prompt)
                response.text ?: ""
            } catch (e: Exception) {
                // In a real application, you would want to handle this exception more gracefully
                throw RuntimeException("Failed to generate content with Gemini API", e)
            }
        }
    }
}
