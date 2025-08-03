package com.hereliesaz.geministrator.core.council

import com.hereliesaz.geministrator.common.GeminiService
import com.hereliesaz.geministrator.common.ILogger

class TechSupport(private val logger: ILogger, private val ai: GeminiService) {
    fun analyzeMergeConflict(conflictOutput: String): String {
        logger.log("Tech Support: Analyzing merge conflict...")
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