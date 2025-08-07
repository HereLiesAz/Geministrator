package com.hereliesaz.geministrator.android.data

import com.hereliesaz.geministrator.android.ui.project.ProjectViewModel
import com.hereliesaz.geministrator.android.ui.session.SessionViewModel
import com.hereliesaz.geministrator.common.AbstractCommand
import com.hereliesaz.geministrator.common.ExecutionAdapter
import com.hereliesaz.geministrator.common.ExecutionResult
import com.hereliesaz.geministrator.common.ILogger
import java.io.File

class AndroidExecutionAdapter(
    private val projectViewModel: ProjectViewModel,
    private val logger: ILogger,
    private val sessionViewModel: SessionViewModel,
) : ExecutionAdapter {

    private val gitManager: GitManager? get() = projectViewModel.gitManager
    private val projectCacheDir: File? get() = projectViewModel.uiState.value.localCachePath

    override suspend fun execute(command: AbstractCommand, silent: Boolean): ExecutionResult {
        if (projectCacheDir == null && command !is AbstractCommand.LogJournalEntry) {
            val message = "Project cache directory is not initialized."
            logger.error(message)
            return ExecutionResult(false, message)
        }

        return when (command) {
            is AbstractCommand.WriteFile -> {
                projectViewModel.writeFile(command.path, command.content).fold(
                    onSuccess = { ExecutionResult(true, "Wrote to ${command.path}") },
                    onFailure = { e ->
                        val message = "Failed to write file ${command.path}: ${e.message}"
                        logger.error(message)
                        ExecutionResult(false, message)
                    }
                )
            }
            is AbstractCommand.ReadFile -> {
                projectViewModel.readFile(command.path).fold(
                    onSuccess = { content ->
                        ExecutionResult(
                            true,
                            "Read file successfully.",
                            content
                        )
                    },
                    onFailure = { e ->
                        val message = "Failed to read file ${command.path}: ${e.message}"
                        logger.error(message)
                        ExecutionResult(false, message)
                    }
                )
            }
            is AbstractCommand.RunTests -> {
                logger.info("Skipping tests in Android environment.")
                ExecutionResult(true, "Tests skipped on Android.")
            }
            is AbstractCommand.GetCurrentBranch -> {
                gitManager?.getCurrentBranch()?.fold(
                    onSuccess = { branchName ->
                        ExecutionResult(
                            true,
                            "Got current branch.",
                            branchName
                        )
                    },
                    onFailure = { e ->
                        ExecutionResult(
                            false,
                            "Failed to get current branch: ${e.message}"
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
                    onFailure = { e ->
                        ExecutionResult(
                            false,
                            "Failed to create and switch branch: ${e.message}"
                        )
                    }
                ) ?: ExecutionResult(false, "GitManager not initialized.")
            }

            is AbstractCommand.SwitchToBranch -> {
                gitManager?.switchToBranch(command.branchName)?.fold(
                    onSuccess = { ExecutionResult(true, "Switched to ${command.branchName}") },
                    onFailure = { e ->
                        ExecutionResult(
                            false,
                            "Failed to switch branch: ${e.message}"
                        )
                    }
                ) ?: ExecutionResult(false, "GitManager not initialized.")
            }
            is AbstractCommand.StageFiles -> {
                gitManager?.stageFiles(command.filePaths)?.fold(
                    onSuccess = { ExecutionResult(true, "Staged files.") },
                    onFailure = { e ->
                        ExecutionResult(
                            false,
                            "Failed to stage files: ${e.message}"
                        )
                    }
                ) ?: ExecutionResult(false, "GitManager not initialized.")
            }
            is AbstractCommand.Commit -> {
                gitManager?.commit(command.message)?.fold(
                    onSuccess = { commitResult ->
                        ExecutionResult(
                            true,
                            "Committed successfully.",
                            commitResult
                        )
                    },
                    onFailure = { e ->
                        val message = "Commit failed: ${e.message}"
                        logger.error(message)
                        ExecutionResult(false, message)
                    }
                ) ?: ExecutionResult(false, "GitManager not initialized.")
            }

            is AbstractCommand.RequestClarification -> {
                val response = sessionViewModel.awaitClarification()
                ExecutionResult(true, "User provided clarification.", response)
            }

            is AbstractCommand.MergeBranch -> {
                gitManager?.mergeBranch(command.branchName)?.fold(
                    onSuccess = { resultMessage -> ExecutionResult(true, resultMessage) },
                    onFailure = { e ->
                        ExecutionResult(
                            false,
                            "Failed to merge branch: ${e.message}"
                        )
                    }
                ) ?: ExecutionResult(false, "GitManager not initialized.")
            }

            is AbstractCommand.DeleteBranch -> {
                gitManager?.deleteBranch(command.branchName)?.fold(
                    onSuccess = { resultMessages ->
                        ExecutionResult(
                            true,
                            "Deleted branch ${command.branchName}",
                            resultMessages
                        )
                    },
                    onFailure = { e ->
                        ExecutionResult(
                            false,
                            "Failed to delete branch: ${e.message}"
                        )
                    }
                ) ?: ExecutionResult(false, "GitManager not initialized.")
            }

            else -> {
                val message = "Command ${command::class.simpleName} is not implemented on Android."
                logger.error(message)
                ExecutionResult(false, message)
            }
        }
    }
}