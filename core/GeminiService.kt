package com.gemini.orchestrator.core

import com.gemini.orchestrator.core.config.ConfigStorage
import com.gemini.orchestrator.core.council.ILogger
import com.gemini.orchestrator.core.tokenizer.Tokenizer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class GeminiService(
    private val apiKey: String,
    private val logger: ILogger,
    private val config: ConfigStorage,
    private val strategicModelName: String,
    private val flashModelName: String
) {
    private val jsonParser = Json { isLenient = true; ignoreUnknownKeys = true }
    private val conversationHistory = mutableListOf<String>()

    fun executeStrategicPrompt(prompt: String): String = executePrompt(prompt, strategicModelName)
    fun executeFlashPrompt(prompt: String): String = executePrompt(prompt, flashModelName)

    private fun executePrompt(prompt: String, model: String): String {
        conversationHistory.add("USER: $prompt")
        val tokenLimit = config.loadTokenLimit()
        val currentTokens = Tokenizer.countTokens(conversationHistory.joinToString("\n"))
        if (currentTokens > tokenLimit) {
            logger.log("⚠️ Token limit reached ($currentTokens / $tokenLimit). Performing graceful session restart.")
            val summaryPrompt = "Summarize the key points and context of the following conversation to preserve memory for a new session:\n\n${conversationHistory.joinToString("\n")}"
            val summary = internalExecute(summaryPrompt, model)
            logger.log("  -> Session summary created.")
            conversationHistory.clear()
            conversationHistory.add("SYSTEM: This is a new session. Here is the summary of the previous one to provide context:\n$summary")
            conversationHistory.add("USER: $prompt")
        }
        val fullPrompt = conversationHistory.joinToString("\n")
        val response = internalExecute(fullPrompt, model)
        conversationHistory.add("AI: $response")
        return response
    }

    private fun internalExecute(prompt: String, model: String): String {
        logger.log("  🧠 Calling AI Model ($model)...")
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("x-goog-api-key", apiKey)
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        val requestBody = """{"model": "$model", "contents": [{"parts":[{"text": "$prompt"}]}]}""".replace("\n", "")
        connection.outputStream.use { it.write(requestBody.toByteArray(Charsets.UTF_8)) }
        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val responseText = connection.inputStream.bufferedReader().readText()
            return try {
                jsonParser.decodeFromString<GeminiResponse>(responseText).candidates.first().content.parts.first().text
            } catch (e: Exception) {
                logger.log("Error parsing Gemini response: $responseText")
                "Error: Could not parse AI response."
            }
        } else {
            throw RuntimeException("API call failed: ${connection.errorStream.bufferedReader().readText()}")
        }
    }

    fun clearSession() { conversationHistory.clear() }

    suspend fun validateApiKey(): Boolean {
        logger.log("  🔑 Validating API Key...")
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey")
        return try {
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val isValid = connection.responseCode == HttpURLConnection.HTTP_OK
            logger.log(if (isValid) "  ✅ API Key is valid." else "  ❌ API Key is invalid.")
            isValid
        } catch (e: Exception) {
            logger.log("  ❌ API Key validation failed with an exception: ${e.message}")
            false
        }
    }

    @Serializable data class GeminiResponse(val candidates: List<Candidate>)
    @Serializable data class Candidate(val content: Content)
    @Serializable data class Content(val parts: List<Part>)
    @Serializable data class Part(val text: String)
}
