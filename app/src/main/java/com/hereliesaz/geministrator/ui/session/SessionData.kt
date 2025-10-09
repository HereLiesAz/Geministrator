package com.hereliesaz.geministrator.ui.session

// A simple enum to represent different agent types
enum class Agent {
    JULES,
    GEMINI,
    USER
}

// Represents a single log entry in a session
data class LogEntry(
    val agent: Agent,
    val message: String,
    val content: String? = null
)

// Represents the final status of a workflow
enum class WorkflowStatus {
    SUCCESS,
    FAILURE
}
