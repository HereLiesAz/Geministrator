package com.hereliesaz.geministrator.android.ui.session

data class SessionUiState(
    val logEntries: List<LogEntry> = emptyList(),
    val status: WorkflowStatus = WorkflowStatus.IDLE,
    val gitStatus: com.hereliesaz.geministrator.android.data.GitStatus? = null,
    val diff: String = ""
)

enum class WorkflowStatus {
    IDLE,
    RUNNING,
    AWAITING_INPUT,
    SUCCESS,
    FAILURE
}