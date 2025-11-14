package com.hereliesaz.geministrator.data

interface JulesRepository {
    suspend fun createSession(repo: String, prompt: String): String
    suspend fun listSessions(): String
    suspend fun pull(sessionId: String): String
}
