package com.hereliesaz.geministrator.core.council

import com.hereliesaz.geministrator.common.AbstractCommand
import com.hereliesaz.geministrator.common.ExecutionAdapter
import com.hereliesaz.geministrator.core.GeminiService
import java.io.File

class Architect(private val logger: ILogger, private val ai: GeminiService, private val adapter: ExecutionAdapter) {
    fun getProjectContextFor(task: String, projectRoot: String): String {
        logger.log("Architect: Performing deep context analysis for '$task'.")
        val fileTree = File(projectRoot).walk().maxDepth(5)
            .filter { it.isFile && !it.path.contains(".git") && !it.path.contains(".idea") }
            .joinToString("\n") { it.relativeTo(File(projectRoot)).path }
        val triagePrompt = """
            You are an expert software architect.
            Your job is to identify the most relevant files for a given task.
            From the following file tree, list the 3-5 most critical files needed to accomplish the task.
            Respond with ONLY a comma-separated list of file paths.

            FILE TREE:
            $fileTree

            TASK: "$task"
        """.trimIndent()
        val relevantFilePaths = ai.executeFlashPrompt(triagePrompt).split(",").map { it.trim() }
        logger.log("  -> Architect identified relevant files: $relevantFilePaths")
        val contextBuilder = StringBuilder("RELEVANT FILE CONTEXT:\n")
        relevantFilePaths.forEach { path ->
            if (path.isNotBlank()) {
                val result = adapter.execute(AbstractCommand.ReadFile(path))
                if (result.isSuccess) {
                    contextBuilder.append("--- FILE: $path ---\n")
                    contextBuilder.append((result.data as? String) ?: "Could not read file.")
                    contextBuilder.append("\n\n")
                }
            }
        }
        return contextBuilder.toString()
    }

    fun reviewStagedChanges(changes: Map<String, String>): Boolean {
        logger.log("Architect: Reviewing ${changes.size} staged files for architectural compliance.")
        val prompt = """
            You are The Architect, an expert on software architecture.
            The following code changes have been proposed. Review them for any potential violations of clean architecture principles, unintended side effects, or major flaws.
            Respond with "APPROVE" if the changes are acceptable, or "REJECT: [reason]" if they are not.
            PROPOSED CHANGES:
            $changes

            Your decision:
        """.trimIndent()
        val decision = ai.executeStrategicPrompt(prompt)
        logger.log("Architect's Decision: $decision")
        return decision.startsWith("APPROVE")
    }
}