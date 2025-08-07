package com.hereliesaz.geministrator.android.data.local

import com.hereliesaz.geministrator.android.ui.session.LogEntry
import com.hereliesaz.geministrator.android.ui.session.WorkflowStatus

class HistoryRepository(private val historyDao: HistoryDao) {

    fun getAllSessions() = historyDao.getAllSessions()

    fun getSessionWithLogs(sessionId: Long) = historyDao.getSessionWithLogs(sessionId)

    suspend fun saveCompletedSession(
        prompt: String,
        status: WorkflowStatus,
        logEntries: List<LogEntry>,
    ) {
        val sessionId = historyDao.insertSession(
            SessionHistoryEntity(
                initialPrompt = prompt,
                endTimestamp = System.currentTimeMillis(),
                status = status.name
            )
        )

        val logEntities = logEntries.map { log ->
            LogEntryEntity(
                sessionId = sessionId,
                agent = log.agent.name,
                message = log.message,
                content = log.content
            )
        }
        historyDao.insertLogEntries(logEntities)
    }
}