package com.hereliesaz.geministrator.data

import com.jules.apiclient.Activity
import com.jules.apiclient.JulesApiClient
import com.jules.apiclient.Session
import com.jules.apiclient.Source
import com.jules.apiclient.Turn
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class JulesRepositoryImpl @Inject constructor(
    private val settingsRepository: SettingsRepository
) : JulesRepository {

    private var apiClient: JulesApiClient? = null

    private suspend fun getClient(): JulesApiClient {
        if (apiClient == null) {
            val apiKey = settingsRepository.apiKey.first()
            if (apiKey.isNullOrBlank()) {
                throw IllegalStateException("Jules API key not found")
            }
            apiClient = JulesApiClient(apiKey)
        }
        return apiClient!!
    }

    override suspend fun getSources(): List<Source> {
        return getClient().getSources().sources
    }

    override suspend fun createSession(prompt: String, source: Source, title: String, context: String): Session {
        return getClient().createSession(prompt, source, title, context)
    }

    override suspend fun nextTurn(sessionId: String, prompt: String): Turn {
        return getClient().nextTurn(sessionId, prompt)
    }

    override suspend fun getSession(sessionId: String): Session {
        return getClient().getSession(sessionId)
    }

    override suspend fun listSessions(): List<Session> {
        return getClient().getSessions()
    }

    override suspend fun approvePlan(sessionId: String) {
        return getClient().approvePlan(sessionId)
    }

    override suspend fun listActivities(sessionId: String): List<Activity> {
        return getClient().getActivities(sessionId).activities
    }
}
