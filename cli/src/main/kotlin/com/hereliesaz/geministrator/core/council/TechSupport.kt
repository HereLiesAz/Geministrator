package com.hereliesaz.geministrator.core.council

import com.hereliesaz.geministrator.common.JulesService
import com.hereliesaz.geministrator.common.ILogger
import com.hereliesaz.geministrator.common.PromptManager

class TechSupport(
    private val logger: ILogger,
    private val ai: JulesService,
    private val promptManager: PromptManager,
) {
    suspend fun analyzeMergeConflict(conflictOutput: String): String {
        logger.info("Tech Support: Analyzing merge conflict...")
        val prompt = promptManager.getPrompt(
            "techSupport.analyzeMergeConflict",
            mapOf("conflictOutput" to conflictOutput)
        )
        return ai.executeStrategicPrompt(prompt)
    }
}