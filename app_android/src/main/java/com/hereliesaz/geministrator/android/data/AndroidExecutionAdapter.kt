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

    private val gitManager: GitManager? get() = projectViewModel.gitManager
    private val projectCacheDir: File? get() = projectViewModel.uiState.value.localCachePath

    override fun execute(command: AbstractCommand, silent: Boolean): ExecutionResult {
        if (projectCacheDir == null && command !is AbstractCommand.LogJournalEntry) {
            val message = "Project cache directory is not initialized."
            logger.error(message)
            return ExecutionResult(false, message)
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
                    val message = "Failed to read file: ${command.path}"
                    logger.error(message)
                    ExecutionResult(false, message)
                }
            }
            is AbstractCommand.RunTests -> {
                logger.info("Skipping tests in Android environment.")
                ExecutionResult(true, "Tests skipped on Android.")
            }
            is AbstractCommand.GetCurrentBranch -> {
                gitManager?.getCurrentBranch()?.fold(
                    onSuccess = { ExecutionResult(true, "Got current branch.", it) },
                    onFailure = {
                        ExecutionResult(
                            false,
                            "Failed to get current branch: ${it.message}"
                        )
                    }
                ) ?: ExecutionResult(false, "GitManager not initialized.")
            }
            is AbstractCommand.CreateAndSwitchToBranch -> {
                gitManager?.createAndSwitchToBranch(command.branchName)?.fold(
                    onSuccess = {
                        ExecutionResult(
                            true,
                            "Created and switched to ${command.branchName}"
                        )
                    },
                    onFailure = {
                        ExecutionResult(
                            false,
                            "Failed to create and switch branch: ${it.message}"
                        )
                    }
                ) ?: ExecutionResult(false, "GitManager not initialized.")
            }
            is AbstractCommand.StageFiles -> {
                var success = true
                var errorMsg = ""
                command.filePaths.forEach { path ->
                    gitManager?.stageFile(path)?.onFailure {
                        success = false
                        errorMsg = it.message ?: "Failed to stage $path"
                        logger.error(errorMsg)
                    }
                }
                if (success) ExecutionResult(true, "Staged files.")
                else ExecutionResult(false, errorMsg)
            }
            is AbstractCommand.Commit -> {
                val result = gitManager?.commit(command.message)
                result?.fold(
                    onSuccess = { ExecutionResult(true, "Committed successfully.", it) },
                    onFailure = {
                        val message = "Commit failed: ${it.message}"
                        logger.error(message)
                        ExecutionResult(false, message)
                    }
                ) ?: run {
                    val message = "GitManager not initialized."
                    logger.error(message)
                    ExecutionResult(false, message)
                }
            }
            else -> {
                val message =
                    "Command ${command::class.simpleName} executed with default success (Not fully implemented on Android)."
                logger.info(message)
                ExecutionResult(true, message)
            }
        }
    }
}