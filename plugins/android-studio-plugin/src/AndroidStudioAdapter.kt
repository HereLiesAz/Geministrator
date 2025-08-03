package com.hereliesaz.geministrator.adapter.as

import com.hereliesaz.geministrator.common.*
import com.hereliesaz.geministrator.core.council.ILogger
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VfsUtil
import java.io.File

class AndroidStudioAdapter(private val project: Project, private val logger: ILogger) : ExecutionAdapter {
    override fun execute(command: AbstractCommand): ExecutionResult {
        var result: ExecutionResult? = null
        ApplicationManager.getApplication().invokeAndWait {
            result = when (command) {
                is AbstractCommand.ReadFile -> {
                    try {
                        val file = VfsUtil.findFileByIoFile(File(project.basePath, command.path), false)
                        val content = file?.let { VfsUtil.loadText(it) } ?: ""
                        ExecutionResult(true, "Read file.", content)
                    } catch (e: Exception) { ExecutionResult(false, "Failed to read file: ${e.message}") }
                }
                is AbstractCommand.RequestCommitReview -> {
                    // This is a simplified version. A real implementation would be more complex.
                    val userChoice = Messages.showYesNoDialog(project, "Approve changes for commit?\nMessage: ${command.proposedCommitMessage}", "Final Review", "Approve", "Reject", null)
                    val decision = if (userChoice == Messages.YES) "APPROVE" else "REJECT"
                    ExecutionResult(true, "User chose '$decision'", decision)
                }
                // This is a simplified adapter. A full implementation would use the IntelliJ SDK
                // for every single command (Git, file I/O, etc.) for maximum safety and integration.
                else -> ExecutionResult(true, "Simulated execution of ${command::class.simpleName} in IDE", null)
            }
        }
        return result ?: ExecutionResult(false, "Adapter command failed to execute on EDT.")
    }
}