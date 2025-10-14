package com.jules.apiclient

import com.google.firebase.vertexai.FirebaseVertexAI
import com.google.firebase.vertexai.type.GenerateContentResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiApiClient(
    private val projectId: String,
    private val location: String,
    private val modelName: String
) {

    suspend fun generateContent(prompt: String): GenerateContentResponse {
        return withContext(Dispatchers.IO) {
            try {
                val model = FirebaseVertexAI.getInstance().generativeModel(modelName)
                val response = model.generateContent(prompt)
                response
            } catch (e: Exception) {
                // In a real application, you would want to handle this exception more gracefully
                throw RuntimeException("Failed to generate content with Gemini API", e)
            }
        }
    }
}
