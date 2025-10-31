package com.hereliesaz.geministrator.data

import com.jules.apiclient.Session
import com.jules.apiclient.Turn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val julesRepository: JulesRepository
) : SessionRepository {

    private val _session = MutableStateFlow<Session?>(null)
    override val session: Flow<Session?> = _session.asStateFlow()

    override suspend fun loadSession(sessionId: String) {
        val session = julesRepository.getSession(sessionId)
        _session.value = session
    }

    override suspend fun sendMessage(prompt: String): Turn {
        val sessionId = _session.value?.id ?: throw IllegalStateException("No active session")
        val newTurn = julesRepository.nextTurn(sessionId, prompt)
        val currentSession = _session.value
        if (currentSession != null) {
            val updatedHistory = currentSession.history.toMutableList()
            updatedHistory.add(newTurn)
            _session.value = currentSession.copy(history = updatedHistory)
        }
        return newTurn
    }
}
