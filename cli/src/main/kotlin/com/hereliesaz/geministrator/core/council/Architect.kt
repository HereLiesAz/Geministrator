package com.hereliesaz.geministrator.core.council

import com.hereliesaz.geministrator.common.AbstractCommand
import com.hereliesaz.geministrator.common.ExecutionAdapter
import com.hereliesaz.geministrator.common.GeminiService
import com.hereliesaz.geministrator.common.ILogger
import com.hereliesaz.geministrator.common.PromptManager
import java.io.File

class Architect(
    private val logger: ILogger,
    private val ai: GeminiService,
    private val adapter: ExecutionAdapter,
    private val promptManager: PromptManager,
) {
    fun getProjectContextFor(task: String, projectRoot: String): String {
        logger.info("Architect: Performing deep context analysis for '$task'.")
        val fileTree = File(projectRoot).walk().maxDepth(5)
            .filter { it.isFile && !it.path.contains(".git") && !it.path.contains(".idea") }
            .joinToString("\n") { it.relativeTo(File(projectRoot)).path }
        val triagePrompt = promptManager.getPrompt(
            "architect.getProjectContext",
            mapOf("fileTree" to fileTree, "task" to task)
        )
        val relevantFilePaths = ai.executeFlashPrompt(triagePrompt).split(",").map { it.trim() }
        logger.info("  -> Architect identified relevant files: $relevantFilePaths")
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
        logger.info("Architect: Reviewing ${changes.size} staged files for architectural compliance.")
        val prompt = promptManager.getPrompt(
            "architect.reviewStagedChanges",
            mapOf("changes" to changes.toString())
        )
        val decision = ai.executeStrategicPrompt(prompt)
        logger.info("Architect's Decision: $decision")
        return decision.startsWith("APPROVE")
    }
}