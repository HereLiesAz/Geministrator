package com.hereliesaz.geministrator.data

import com.jules.apiclient.Activity
import com.jules.apiclient.Session
import com.jules.apiclient.Source
import com.jules.apiclient.Turn

interface JulesRepository {
    suspend fun getSources(): List<Source>
    suspend fun createSession(prompt: String, source: Source, title: String, context: String): Session
    suspend fun nextTurn(sessionId: String, prompt: String): Turn
    suspend fun getSession(sessionId: String): Session
    suspend fun listSessions(): List<Session>
    suspend fun approvePlan(sessionId: String)
    suspend fun listActivities(sessionId: String): List<Activity>
    suspend fun getFileContent(sessionId: String, filePath: String): String
    suspend fun runFile(sessionId: String, filePath: String): String
    suspend fun updateFileContent(sessionId: String, filePath: String, content: String)
    suspend fun commitChanges(sessionId: String, message: String): String
}
