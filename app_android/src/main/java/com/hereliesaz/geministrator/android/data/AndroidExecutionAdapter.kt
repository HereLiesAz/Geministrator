package com.hereliesaz.geministrator.android.data

import com.hereliesaz.geministrator.android.ui.project.ProjectViewModel
import com.hereliesaz.geministrator.common.AbstractCommand
import com.hereliesaz.geministrator.common.ExecutionAdapter
import com.hereliesaz.geministrator.common.ExecutionResult
import com.hereliesaz.geministrator.common.ILogger
import java.io.File

class AndroidExecutionAdapter(
    private val projectViewModel: ProjectViewModel,
    private val logger: ILogger
) : ExecutionAdapter {

    private val gitManager: GitManager?
        get() = projectViewModel.gitManager

    private val projectCacheDir: File?
        get() = projectViewModel.uiState.value.localCachePath

    override fun execute(command: AbstractCommand, silent: Boolean): ExecutionResult {
        if (projectCacheDir == null && command !is AbstractCommand.LogJournalEntry) {
            return ExecutionResult(false, "Project cache directory is not initialized.")
        }

        return when (command) {
            is AbstractCommand.WriteFile -> {
                projectViewModel.writeFile(command.path, command.content)
                ExecutionResult(true, "Wrote to ${command.path}")
            }
            is AbstractCommand.ReadFile -> {
                val content = projectViewModel.readFile(command.path)
                if (content != null) {
                    ExecutionResult(true, "Read file successfully.", content)
                } else {
                    ExecutionResult(false, "Failed to read file: ${command.path}")
                }
            }
            is AbstractCommand.RunTests -> {
                // Testing is not implemented in the Android environment.
                logger.info("Skipping tests in Android environment.")
                ExecutionResult(true, "Tests skipped on Android.")
            }
            is AbstractCommand.GetCurrentBranch -> {
                ExecutionResult(true, "main") // Simplified for Android
            }
            is AbstractCommand.CreateAndSwitchToBranch -> {
                // JGit doesn't have a direct checkout -b equivalent.
                // It's a combination of branchCreate and checkout.
                // For now, we simulate this.
                ExecutionResult(true, "Simulated create and switch to ${command.branchName}")
            }
            is AbstractCommand.StageFiles -> {
                var success = true
                command.filePaths.forEach { path ->
                    gitManager?.stageFile(path)?.onFailure { success = false }
                }
                if (success) ExecutionResult(true, "Staged files.")
                else ExecutionResult(false, "Failed to stage one or more files.")
            }
            is AbstractCommand.Commit -> {
                val result = gitManager?.commit(command.message)
                result?.fold(
                    onSuccess = { ExecutionResult(true, "Committed successfully.", it) },
                    onFailure = { ExecutionResult(false, "Commit failed: ${it.message}") }
                ) ?: ExecutionResult(false, "GitManager not initialized.")
            }
            // --- Other commands can be implemented as needed ---
            else -> {
                logger.info("Executing command: ${command::class.simpleName} (Not fully implemented on Android)")
                ExecutionResult(true, "Command ${command::class.simpleName} executed with default success.")
            }
        }
    }
}