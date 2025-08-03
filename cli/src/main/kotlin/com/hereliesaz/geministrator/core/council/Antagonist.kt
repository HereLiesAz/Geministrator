package com.hereliesaz.geministrator.core.council

import com.hereliesaz.geministrator.common.GeminiService
import com.hereliesaz.geministrator.common.ILogger
import com.hereliesaz.geministrator.common.PromptManager

class Antagonist(
    private val logger: ILogger,
    private val ai: GeminiService,
    private val promptManager: PromptManager,
) {
    fun reviewPlan(planJson: String): String? {
        logger.info("Antagonist: Reviewing the proposed workflow...")
        val prompt = promptManager.getPrompt("antagonist.reviewPlan", mapOf("planJson" to planJson))
        val review = ai.executeStrategicPrompt(prompt)
        if (review.startsWith("OBJECTION:")) {
            logger.info("Antagonist: $review")
            return review
        }
        logger.info("Antagonist: The plan seems reasonable. No objections.")
        return null
    }
}