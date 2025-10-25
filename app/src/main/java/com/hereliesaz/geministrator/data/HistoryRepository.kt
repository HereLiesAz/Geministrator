package com.hereliesaz.geministrator.data

import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface HistoryRepository {
    fun getSessionHistory(sessionId: UUID): Flow<List<String>>
    suspend fun addMessageToHistory(sessionId: UUID, message: String)
}
