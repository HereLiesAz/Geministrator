package com.hereliesaz.GeminiOrchestrator.core.council

import com.hereliesaz.GeminiOrchestrator.common.AbstractCommand
import com.hereliesaz.GeminiOrchestrator.common.ExecutionAdapter
import com.hereliesaz.GeminiOrchestrator.core.GeminiService
import java.io.File

interface ILogger { fun log(message: String) }

class Architect(private val logger: ILogger, private val ai: GeminiService, private val adapter: ExecutionAdapter) {
    fun getProjectContextFor(task: String, projectRoot: String): String {
        logger.log("ðŸ›ï¸ Architect: Performing deep context analysis for '$task'.")
        val fileTree = File(projectRoot).walk().maxDepth(5)
            .filter { it.isFile && !it.path.contains(".git") && !it.path.contains(".idea") }
            .joinToString("\n") { it.relativeTo(File(projectRoot)).path }
        val triagePrompt = """
            You are an expert software architect. Your job is to identify the most relevant files for a given task.
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
        logger.log("ðŸ›ï¸ Architect: Reviewing ${changes.size} staged files for architectural compliance.")
        val prompt = """
            You are The Architect, an expert on software architecture.
            The following code changes have been proposed. Review them for any potential violations of clean architecture principles, unintended side effects, or major flaws.
            Respond with "APPROVE" if the changes are acceptable, or "REJECT: [reason]" if they are not.

            PROPOSED CHANGES:
            $changes

            Your decision:
        """.trimIndent()
        val decision = ai.executeStrategicPrompt(prompt)
        logger.log("ðŸ›ï¸ Architect's Decision: $decision")
        return decision.startsWith("APPROVE")
    }
}

class Researcher(private val logger: ILogger, private val ai: GeminiService, private val adapter: ExecutionAdapter) {
    fun findBestPracticesFor(topic: String): String {
        logger.log("ðŸ“š Researcher: Searching for best practices regarding '$topic'.")
        val searchResult = adapter.execute(AbstractCommand.PerformWebSearch(topic))
        val prompt = """
            You are a Senior Staff Engineer. Based on the following web search results,
            summarize the current best practices for the topic.

            SEARCH RESULTS:
            ${searchResult.output}

            TOPIC: "$topic"
        """.trimIndent()
        return ai.executeFlashPrompt(prompt)
    }
}

class Designer(private val logger: ILogger, private val adapter: ExecutionAdapter) {
    fun createSpecification(feature: String): List<AbstractCommand> {
        logger.log("ðŸŽ¨ Designer: Creating feature specification for '$feature'.")
        return listOf(AbstractCommand.WriteFile(
            path = "docs/specs/${feature.replace(" ", "_")}.md",
            content = "# Feature: $feature\n\nThis feature should allow users to..."
        ))
    }
    fun updateChangelog(commitMessage: String) {
        logger.log("ðŸŽ¨ Designer: Updating changelog.")
        adapter.execute(AbstractCommand.AppendToFile("CHANGELOG.md", "\n- $commitMessage"))
    }
    fun recordHistoricalLesson(lesson: String) {
        logger.log("ðŸŽ¨ Designer: Recording important lesson in project history.")
        adapter.execute(AbstractCommand.AppendToFile("docs/history.md", "\n- $lesson"))
    }
}

class Antagonist(private val logger: ILogger, private val ai: GeminiService) {
    fun reviewPlan(planJson: String): String? {
        logger.log("ðŸ¤” Antagonist: Reviewing the proposed workflow...")
        val prompt = """
            You are The Antagonist, a cynical but brilliant principal engineer. Your only goal is to find flaws in proposed plans.
            Critique the following workflow plan. Look for missing steps (especially testing), inefficiencies, or potential risks.
            If you find a critical flaw, respond with "OBJECTION: [Your reason]".
            If the plan is sound, respond with "APPROVE".

            PROPOSED PLAN (in JSON):
            $planJson
        """.trimIndent()
        val review = ai.executeStrategicPrompt(prompt)
        if (review.startsWith("OBJECTION:")) {
            logger.log("ðŸ”¥ Antagonist: $review")
            return review
        }
        logger.log("ðŸ‘ Antagonist: The plan seems reasonable. No objections.")
        return null
    }
}

class TechSupport(private val logger: ILogger, private val ai: GeminiService) {
    fun analyzeMergeConflict(conflictOutput: String): String {
        logger.log("ðŸ“ž Tech Support: Analyzing merge conflict...")
        val prompt = """
            You are a Tech Support specialist for a team of AI agents.
            The following 'git merge' command failed. Analyze the conflict output and explain the root cause.
            Propose a clear, step-by-step strategy for how another AI agent could resolve this conflict.

            CONFLICT OUTPUT:
            $conflictOutput

            Your analysis and resolution plan:
        """.trimIndent()
        return ai.executeStrategicPrompt(prompt)
    }
}

