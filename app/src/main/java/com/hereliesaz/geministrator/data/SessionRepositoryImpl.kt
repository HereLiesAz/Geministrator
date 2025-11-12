package com.hereliesaz.geministrator.data

import com.hereliesaz.julesapisdk.Session
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

    override suspend fun sendMessage(prompt: String) {
        val sessionId = _session.value?.id ?: throw IllegalStateException("No active session")
        julesRepository.sendMessage(sessionId, prompt)
        loadSession(sessionId)
    }
}
