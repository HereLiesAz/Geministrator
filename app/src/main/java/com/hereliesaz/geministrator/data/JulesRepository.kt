package com.hereliesaz.geministrator.data

import com.hereliesaz.julesapisdk.Activity
import com.hereliesaz.julesapisdk.Session
import com.hereliesaz.julesapisdk.Source

interface JulesRepository {
    suspend fun getSources(): List<Source>
    suspend fun createSession(prompt: String, source: Source, title: String): Session
    suspend fun getSession(sessionId: String): Session
    suspend fun listSessions(): List<Session>
    suspend fun approvePlan(sessionId: String)
    suspend fun listActivities(sessionId: String): List<Activity>
    suspend fun sendMessage(sessionId: String, prompt: String)
}
