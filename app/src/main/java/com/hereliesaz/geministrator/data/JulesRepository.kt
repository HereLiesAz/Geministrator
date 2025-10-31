package com.hereliesaz.geministrator.data

import com.jules.apiclient.Session
import com.jules.apiclient.Source
import com.jules.apiclient.Turn

interface JulesRepository {
    suspend fun getSources(): List<Source>
    suspend fun createSession(prompt: String, source: Source, title: String, context: String): Session
    suspend fun nextTurn(sessionId: String, prompt: String): Turn
    suspend fun getSession(sessionId: String): Session
}
