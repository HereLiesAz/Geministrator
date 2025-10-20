package com.jules.apiclient

/**
 * A2ACommunicator is responsible for orchestrating communication between
 * the Jules and Gemini API clients. It will translate requests and responses
 * to enable them to work together.
 */
class A2ACommunicator(
    private val julesApiClient: JulesApiClient,
    private val geminiApiClient: GeminiApiClient
) {

    /**
     * Has Jules ask a question to the Gemini agent.
     */
    suspend fun julesToGemini(prompt: String): String {
        return geminiApiClient.generateContent(prompt)
    }

    /**
     * Has the Gemini agent send a message to a Jules session.
     */
    suspend fun geminiToJules_sendMessage(sessionId: String, prompt: String) {
        julesApiClient.sendMessage(sessionId, prompt)
    }
}
