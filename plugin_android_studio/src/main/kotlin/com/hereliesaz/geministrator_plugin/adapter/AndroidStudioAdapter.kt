package com.hereliesaz.geministrator_plugin.adapter

import com.hereliesaz.geministrator.common.AbstractCommand
import com.hereliesaz.geministrator.common.ExecutionAdapter
import com.hereliesaz.geministrator.common.ExecutionResult
import com.hereliesaz.geministrator.common.ILogger
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class AndroidStudioAdapter(private val project: Project, private val logger: ILogger) : ExecutionAdapter {
    private val gitHandler = GitHandler(project, logger)

    override suspend fun execute(command: AbstractCommand, silent: Boolean): ExecutionResult {
        return when (command) {
            // File System
            is AbstractCommand.ReadFile -> readFile(command.path)
            is AbstractCommand.WriteFile -> writeFile(command.path, command.content)
            is AbstractCommand.AppendToFile -> appendToFile(command.path, command.content)
            is AbstractCommand.DeleteFile -> deleteFile(command.path)

            // Execution
            is AbstractCommand.RunShellCommand -> runShellCommand(
                command.command,
                command.workingDir
            )

            is AbstractCommand.RunTests -> runTests(command.module, command.testName)

            // User Interaction
            is AbstractCommand.RequestCommitReview -> requestCommitReview(command.proposedCommitMessage)
            is AbstractCommand.RequestClarification -> requestClarification(command.question)
            is AbstractCommand.RequestUserDecision -> requestUserDecision(
                command.prompt,
                command.options
            )

            // Version Control
            is AbstractCommand.GetCurrentBranch -> gitHandler.getCurrentBranch()
            is AbstractCommand.CreateAndSwitchToBranch -> gitHandler.createAndSwitchToBranch(command.branchName)
            is AbstractCommand.SwitchToBranch -> gitHandler.switchToBranch(command.branchName)
            is AbstractCommand.MergeBranch -> gitHandler.mergeBranch(command.branchName)
            is AbstractCommand.DeleteBranch -> gitHandler.deleteBranch(command.branchName)
            is AbstractCommand.StageFiles -> gitHandler.stageFiles(command.filePaths)
            is AbstractCommand.Commit -> gitHandler.commit(command.message)

            // Default fallback for unimplemented commands
            else -> {
                val message =
                    "Command ${command::class.simpleName} is not implemented in the Android Studio plugin."
                logger.error(message)
                ExecutionResult(false, message)
            }
        }
    }

    private suspend fun runShellCommand(command: List<String>, workDir: String): ExecutionResult =
        withContext(Dispatchers.IO) {
            try {
                val workingDir = project.basePath?.let { File(it, workDir) } ?: File(workDir)
                if (!workingDir.exists()) {
                    return@withContext ExecutionResult(
                        false,
                        "Working directory does not exist: ${workingDir.absolutePath}"
                    )
                }

                logger.info("Executing: `${command.joinToString(" ")}` in `${workingDir.name}`")

                val commandLine =
                    GeneralCommandLine(command).withWorkDirectory(workingDir.absolutePath)
                val handler = OSProcessHandler(commandLine)
                val output = StringBuilder()

                handler.addProcessListener(object : ProcessAdapter() {
                    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                        val text = event.text
                        output.append(text)
                        // Log in real-time to the UI
                        logger.info(text.trimEnd())
                    }

                    override fun processTerminated(event: ProcessEvent) {
                        logger.info("\nProcess finished with exit code ${event.exitCode}")
                    }
                })

                handler.startNotify()
                // Wait for the process to complete. A timeout can be added here.
                handler.waitFor()

                if (handler.exitCode == 0) {
                    ExecutionResult(
                        true,
                        output.toString().ifBlank { "Command executed successfully." })
                } else {
                    ExecutionResult(false, "Exit code ${handler.exitCode}: ${output}")
                }
            } catch (e: Exception) {
                val message = "Failed to execute shell command: ${e.message}"
                logger.error(message, e)
                ExecutionResult(false, message)
            }
        }

    private suspend fun runTests(module: String?, testName: String?): ExecutionResult {
        val cmd = mutableListOf("./gradlew")
        val task = if (module != null) ":${module}:test" else "test"
        cmd.add(task)
        testName?.let {
            cmd.add("--tests")
            cmd.add(it)
        }
        cmd.add("--info") // Add info for more detailed output
        return runShellCommand(cmd, ".")
    }

    private suspend fun writeFile(path: String, content: String): ExecutionResult =
        withContext(Dispatchers.IO) {
            try {
                val projectBase = project.basePath ?: return@withContext ExecutionResult(
                    false,
                    "Project path not found."
                )
                val file = File(projectBase, path)
                val virtualFile = VfsUtil.createDirectories(file.parent)
                    .findOrCreateChildData(this, file.name)

                WriteAction.runAndWait<IOException> {
                    VfsUtil.saveText(virtualFile, content)
                }
                ExecutionResult(true, "Wrote to $path")
            } catch (e: IOException) {
                ExecutionResult(false, "Failed to write file: ${e.message}")
            }
        }

    private suspend fun readFile(path: String): ExecutionResult = withContext(Dispatchers.IO) {
        try {
            val file = VfsUtil.findFileByIoFile(File(project.basePath ?: "", path), true)
                ?: return@withContext ExecutionResult(false, "File not found: $path")
            val content = VfsUtil.loadText(file)
            ExecutionResult(true, "Read file.", content)
        } catch (e: Exception) {
            ExecutionResult(false, "Failed to read file: ${e.message}")
        }
    }

    private suspend fun appendToFile(path: String, content: String): ExecutionResult =
        withContext(Dispatchers.IO) {
            try {
                val file = VfsUtil.findFileByIoFile(File(project.basePath ?: "", path), true)
                    ?: return@withContext ExecutionResult(false, "File not found: $path")
                WriteAction.runAndWait<IOException> {
                    VfsUtil.saveText(file, VfsUtil.loadText(file) + content)
                }
                ExecutionResult(true, "Appended to $path")
            } catch (e: IOException) {
                ExecutionResult(false, "Failed to append to file: ${e.message}")
            }
        }

    private suspend fun deleteFile(path: String): ExecutionResult = withContext(Dispatchers.IO) {
        try {
            val file = VfsUtil.findFileByIoFile(File(project.basePath ?: "", path), true)
                ?: return@withContext ExecutionResult(true, "File already deleted: $path")
            WriteAction.runAndWait<IOException> {
                file.delete(this)
            }
            ExecutionResult(true, "Deleted $path")
        } catch (e: IOException) {
            ExecutionResult(false, "Failed to delete file: ${e.message}")
        }
    }

    private suspend fun requestCommitReview(proposedCommitMessage: String): ExecutionResult {
        val userChoice = withContext(Dispatchers.Main) {
            Messages.showYesNoDialog(
                project,
                "Approve changes for commit?\nMessage: $proposedCommitMessage",
                "Final Review",
                "Approve",
                "Reject",
                null
            )
        }
        val decision = if (userChoice == Messages.YES) "APPROVE" else "REJECT"
        return ExecutionResult(true, "User chose '$decision'", decision)
    }

    private suspend fun requestClarification(question: String): ExecutionResult {
        val response = withContext(Dispatchers.Main) {
            Messages.showInputDialog(
                project,
                question,
                "Clarification Required",
                Messages.getQuestionIcon()
            )
        }
        return ExecutionResult(true, "User provided clarification.", response)
    }

    private suspend fun requestUserDecision(
        prompt: String,
        options: List<String>,
    ): ExecutionResult {
        val choice = withContext(Dispatchers.Main) {
            Messages.showChooseDialog(
                prompt,
                "User Decision Required",
                options.toTypedArray(),
                options.first(),
                null
            )
        }
        return ExecutionResult(true, "User chose '$choice'", choice)
    }
}