package com.hereliesaz.geministrator.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor() : HistoryRepository {

    private val sessionHistories = mutableMapOf<UUID, MutableStateFlow<List<String>>>()

    override fun getSessionHistory(sessionId: UUID): Flow<List<String>> {
        return sessionHistories.getOrPut(sessionId) { MutableStateFlow(emptyList()) }.asStateFlow()
    }

    override suspend fun addMessageToHistory(sessionId: UUID, message: String) {
        val history = sessionHistories.getOrPut(sessionId) { MutableStateFlow(emptyList()) }
        history.value = history.value + message
    }
}
