package com.hereliesaz.geministrator.agent

import com.google.adk.Agent
import com.hereliesaz.geministrator.data.SettingsRepository
import com.jules.apiclient.GeminiApiClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class GeministratorAgent(
    private val settingsRepository: SettingsRepository
) : Agent {
    override fun execute(request: Agent.Request): Agent.Response {
        val prompt = request.toString() // This is a placeholder
        val responseText = runBlocking {
            val gcpProjectId = settingsRepository.githubRepository.first()
            val gcpLocation = settingsRepository.gcpLocation.first()
            val geminiModelName = settingsRepository.geminiModelName.first()

            if (gcpProjectId.isNullOrEmpty() || gcpLocation.isNullOrEmpty() || geminiModelName.isNullOrEmpty()) {
                "Gemini settings not found. Please configure them in the settings screen."
            } else {
                val geminiApiClient = GeminiApiClient(gcpProjectId, gcpLocation, geminiModelName)
                val response = geminiApiClient.generateContent(prompt)
                response.toString()
            }
        }
        return Agent.Response.newBuilder().setText(responseText).build()
    }
}
