package com.hereliesaz.geministrator.core

import com.hereliesaz.geministrator.core.config.ConfigStorage
import com.hereliesaz.geministrator.core.council.ILogger
import com.hereliesaz.geministrator.core.tokenizer.Tokenizer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URI

// Request/Response structures for serialization
@Serializable private data class GeminiRequest(val model: String, val contents: List<Content>)
@Serializable private data class GeminiResponse(val candidates: List<Candidate>)
@Serializable data class Candidate(val content: Content)
@Serializable data class Content(val parts: List<Part>, val role: String? = null)
@Serializable data class Part(val text: String)


class GeminiService(
    private val apiKey: String,
    private val logger: ILogger,
    private val config: ConfigStorage,
    private val strategicModelName: String,
    private val flashModelName: String
) {
    private val jsonParser = Json { isLenient = true; ignoreUnknownKeys = true }
    private val conversationHistory = mutableListOf<Content>()

    fun executeStrategicPrompt(prompt: String): String = executePrompt(prompt, strategicModelName)
    fun executeFlashPrompt(prompt: String): String = executePrompt(prompt, flashModelName)

    private fun executePrompt(prompt: String, model: String): String {
        conversationHistory.add(Content(parts = listOf(Part(prompt)), role = "user"))

        val tokenLimit = config.loadTokenLimit()
        // Re-calculate tokens based on the JSON representation of the history
        val currentTokens = Tokenizer.countTokens(jsonParser.encodeToString(ListSerializer(Content.serializer()), conversationHistory))

        if (currentTokens > tokenLimit) {
            logger.log("WARNING: Token limit reached ($currentTokens / $tokenLimit). Performing graceful session restart.")
            val historyText = conversationHistory.joinToString("\n") { c -> "${c.role}: ${c.parts.first().text}" }
            val summaryPrompt = "Summarize the key points and context of the following conversation to preserve memory for a new session:\n\n$historyText"
            val summary = internalExecute(summaryPrompt, model)
            logger.log("  -> Session summary created.")
            conversationHistory.clear()
            conversationHistory.add(Content(parts = listOf(Part("This is a new session. Here is the summary of the previous one to provide context:\n$summary")), role = "user"))
            conversationHistory.add(Content(parts = listOf(Part(prompt)), role = "user"))
        }

        val responseText = internalExecute(prompt, model, conversationHistory)
        conversationHistory.add(Content(parts = listOf(Part(responseText)), role = "model"))
        return responseText
    }

    private fun internalExecute(prompt: String, model: String, history: List<Content>? = null): String {
        logger.log("  -> Calling AI Model ($model)...")
        // Use a dynamic endpoint based on the model
        val url =
            URI("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent").toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("x-goog-api-key", apiKey)
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val request = GeminiRequest(model, history ?: listOf(Content(parts=listOf(Part(prompt)))))
        val requestBody = jsonParser.encodeToString(GeminiRequest.serializer(), request)

        connection.outputStream.use { it.write(requestBody.toByteArray(Charsets.UTF_8)) }

        return if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val responseText = connection.inputStream.bufferedReader().readText()
            try {
                jsonParser.decodeFromString<GeminiResponse>(responseText).candidates.first().content.parts.first().text
            } catch (e: Exception) {
                logger.log("Error parsing Gemini response: $responseText")
                "Error: Could not parse AI response."
            }
        } else {
            val error = connection.errorStream.bufferedReader().readText()
            logger.log("API call failed: $error")
            throw RuntimeException("API call failed: $error")
        }
    }

    fun clearSession() { conversationHistory.clear() }

    suspend fun validateApiKey(): Boolean {
        logger.log("  -> Validating API Key...")
        val url = URI("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey").toURL()
        return try {
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val isValid = connection.responseCode == HttpURLConnection.HTTP_OK
            logger.log(if (isValid) "  -> API Key is valid." else "  -> API Key is invalid.")
            isValid
        } catch (e: Exception) {
            logger.log("  -> API Key validation failed with an exception: ${e.message}")
            false
        }
    }
}