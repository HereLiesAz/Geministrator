package com.hereliesaz.geministrator.data

import com.jules.cliclient.JulesCliClient
import javax.inject.Inject

class JulesRepositoryImpl @Inject constructor(
    private val julesCliClient: JulesCliClient
) : JulesRepository {

    override suspend fun createSession(repo: String, prompt: String): String {
        return julesCliClient.newSession(repo, prompt)
    }

    override suspend fun listSessions(): String {
        return julesCliClient.listSessions()
    }

    override suspend fun pull(sessionId: String): String {
        return julesCliClient.pull(sessionId)
    }
}
