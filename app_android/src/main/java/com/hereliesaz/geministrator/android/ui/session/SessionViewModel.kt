package com.hereliesaz.geministrator.android.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.android.ui.project.ProjectViewModel
import com.hereliesaz.geministrator.android.ui.theme.Agent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionViewModel(
    private val prompt: String,
    private val projectViewModel: ProjectViewModel
) : ViewModel() {

    private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries = _logEntries.asStateFlow()

    init {
        startOrchestration()
    }

    private fun addLogEntry(entry: LogEntry) {
        _logEntries.update { it + entry }
    }

    private fun startOrchestration() {
        viewModelScope.launch {
            val git = projectViewModel.gitManager

            addLogEntry(LogEntry("Starting session for prompt: \"$prompt\"", Agent.ORCHESTRATOR))
            delay(1000)

            addLogEntry(LogEntry("Creating new feature file...", Agent.DESIGNER))
            val newFileName = "new_feature.md"
            val newFileContent = "# New Feature\n\nImplemented based on prompt: \"$prompt\""
            projectViewModel.writeFile(newFileName, newFileContent)
            addLogEntry(LogEntry("Wrote content to `$newFileName`.", Agent.DESIGNER))
            delay(1000)

            addLogEntry(LogEntry("Staging changes...", Agent.MANAGER))
            git?.stageFile(newFileName)
            delay(500)

            addLogEntry(LogEntry("Checking repository status...", Agent.ARCHITECT))
            val status = git?.getStatus()?.getOrDefault("Error getting status.") ?: "Git not initialized."
            addLogEntry(LogEntry("Git status retrieved.", Agent.ARCHITECT, content = "```\n$status```"))
            delay(1000)

            addLogEntry(LogEntry("Committing changes...", Agent.MANAGER))
            val commitResult = git?.commit("feat: Implement '$prompt'")?.getOrDefault("Error committing.")
            addLogEntry(LogEntry("Committed with message: \"$commitResult\"", Agent.MANAGER))
        }
    }
}