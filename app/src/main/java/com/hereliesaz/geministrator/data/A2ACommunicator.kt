package com.hereliesaz.geministrator.data

import com.hereliesaz.geministrator.apis.GeminiApiClient
import com.jules.apiclient.JulesApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class A2ACommunicator(
    private val julesApiClient: JulesApiClient,
    private val geminiApiClient: GeminiApiClient
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun sendMessage(sessionId: String, prompt: String, onResult: (String) -> Unit) {
        scope.launch {
            try {
                val response = geminiApiClient.generateContent(prompt)
                onResult(response)
            } catch (e: Exception) {
                onResult("Error: ${e.message}")
            }
        }
    }
}
