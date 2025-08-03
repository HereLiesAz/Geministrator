package com.hereliesaz.GeminiOrchestrator.core.council

import com.hereliesaz.GeminiOrchestrator.core.GeminiService

class Antagonist(private val logger: ILogger, private val ai: GeminiService) {
    fun reviewPlan(planJson: String): String? {
        logger.log("🤔 Antagonist: Reviewing the proposed workflow...")
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
            logger.log("🔥 Antagonist: $review")
            return review
        }
        logger.log("👍 Antagonist: The plan seems reasonable. No objections.")
        return null
    }
}