package com.hereliesaz.geministrator.android.data

import com.hereliesaz.geministrator.android.ui.session.LogEntry
import com.hereliesaz.geministrator.android.ui.theme.Agent
import com.hereliesaz.geministrator.common.ILogger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AndroidLogger : ILogger {

    private val _logFlow = MutableSharedFlow<LogEntry>()
    val logFlow = _logFlow.asSharedFlow()

    // A regex to find an agent name at the start of a string, possibly in brackets.
    private val agentRegex = Regex("""^\[?(\w+)]?[:\s]""")

    private fun parseAndEmit(rawMessage: String, defaultAgent: Agent) {
        val match = agentRegex.find(rawMessage)
        val agent = match?.groups?.get(1)?.let { Agent.fromString(it.value) } ?: defaultAgent
        val message = rawMessage.removePrefix(match?.value ?: "")

        // A simple heuristic to detect final status messages
        val statusMessage = when {
            "Workflow Finished" in message -> "Workflow Finished"
            "critical error occurred" in message -> "Workflow Failed"
            else -> null
        }

        val entry = LogEntry(message = message, agent = agent)

        // In a real implementation, you'd emit status changes separately
        // For now, we piggyback on the log message
        if (statusMessage != null) {
            _logFlow.tryEmit(entry.copy(message = statusMessage)) // Emit final status
        } else {
            _logFlow.tryEmit(entry)
        }
    }

    override fun info(message: String) {
        parseAndEmit(message, Agent.ORCHESTRATOR)
    }

    override fun error(message: String, e: Throwable?) {
        val fullMessage = if (e != null) "$message: ${e.message}" else message
        parseAndEmit(fullMessage, Agent.ANTAGONIST) // Default errors to the Antagonist
    }

    override fun interactive(message: String) {
        // For now, treat interactive messages as info logs from the orchestrator
        parseAndEmit(message, Agent.ORCHESTRATOR)
    }

    override fun prompt(message: String): String? {
        // This logger is non-blocking. It cannot fulfill the prompt contract.
        // It will log the prompt and the real implementation should show a UI.
        val entry = LogEntry(
            message = "Awaiting user input...",
            agent = Agent.ORCHESTRATOR,
            clarificationQuestion = message
        )
        _logFlow.tryEmit(entry)
        return null // Immediately return null as we cannot block the orchestrator thread
    }
}