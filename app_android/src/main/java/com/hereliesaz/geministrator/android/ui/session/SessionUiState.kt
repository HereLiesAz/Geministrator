package com.hereliesaz.geministrator.android.ui.session

data class GitStatusState(
    val modified: List<String> = emptyList(),
    val untracked: List<String> = emptyList(),
    val added: List<String> = emptyList(),
    val removed: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val selectedForStaging: Set<String> = emptySet(),
)

data class DiffViewState(
    val filePath: String,
    val diffContent: String,
)

data class SessionUiState(
    val logEntries: List<LogEntry> = emptyList(),
    val status: WorkflowStatus = WorkflowStatus.IDLE,
    val clarificationPrompt: String? = null,
    val gitStatus: GitStatusState = GitStatusState(),
    val diffViewState: DiffViewState? = null,
)

enum class WorkflowStatus {
    IDLE,
    RUNNING,
    AWAITING_INPUT,
    SUCCESS,
    FAILURE
}