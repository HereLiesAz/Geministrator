package com.hereliesaz.geministrator.data

import com.jules.apiclient.Session
import com.jules.apiclient.Turn
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    val session: Flow<Session?>
    suspend fun loadSession(sessionId: String)
    suspend fun sendMessage(prompt: String): Turn
}
