package com.jules.apiclient

import com.google.cloud.vertexai.VertexAI
import com.google.cloud.vertexai.api.GenerateContentResponse
import com.google.cloud.vertexai.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiApiClient(
    private val projectId: String,
    private val location: String,
    private val modelName: String
) {

    @Suppress("DEPRECATION")
    suspend fun generateContent(prompt: String): GenerateContentResponse {
        return withContext(Dispatchers.IO) {
            try {
                VertexAI(projectId, location).use { vertexAI ->
                    val model = GenerativeModel(modelName, vertexAI)
                    val response = model.generateContent(prompt)
                    response
                }
            } catch (e: Exception) {
                // In a real application, you would want to handle this exception more gracefully
                throw RuntimeException("Failed to generate content with Gemini API", e)
            }
        }
    }
}
