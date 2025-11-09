package com.hereliesaz.geministrator.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class A2ACommunicator @Inject constructor(
    private val julesRepository: JulesRepository,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Sends a prompt to a remote agent and returns the response via a callback.
     */
    fun sendMessage(
        sessionId: String,
        prompt: String,
        onResponse: (String) -> Unit
    ) {
        scope.launch {
            try {
                val response = julesRepository.nextTurn(sessionId, prompt)
                onResponse(response.toString())
            } catch (e: Exception) {
                onResponse("Error: ${e.message}")
            }
        }
    }
}