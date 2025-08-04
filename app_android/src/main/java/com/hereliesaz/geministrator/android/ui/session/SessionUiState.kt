package com.hereliesaz.geministrator.android.ui.session

data class SessionUiState(
    val logEntries: List<LogEntry> = emptyList(),
    val status: WorkflowStatus = WorkflowStatus.IDLE
)

enum class WorkflowStatus {
    IDLE,
    RUNNING,
    AWAITING_INPUT,
    SUCCESS,
    FAILURE
}