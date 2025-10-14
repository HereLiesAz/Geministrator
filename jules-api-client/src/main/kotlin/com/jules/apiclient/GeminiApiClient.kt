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

    private val vertexAI = VertexAI(projectId, location)
    private val generativeModel = GenerativeModel(modelName, vertexAI)
    suspend fun generateContent(prompt: String): GenerateContentResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(prompt)
                response
            } catch (e: Exception) {
                // In a real application, you would want to handle this exception more gracefully
                throw RuntimeException("Failed to generate content with Gemini API", e)
            }
        }
    }
}
