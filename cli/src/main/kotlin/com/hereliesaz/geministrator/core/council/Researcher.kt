package com.hereliesaz.geministrator.core.council

import com.hereliesaz.geministrator.common.AbstractCommand
import com.hereliesaz.geministrator.common.ExecutionAdapter
import com.hereliesaz.geministrator.common.GeminiService
import com.hereliesaz.geministrator.common.ILogger
import com.hereliesaz.geministrator.common.PromptManager

class Researcher(
    private val logger: ILogger,
    private val ai: GeminiService,
    private val adapter: ExecutionAdapter,
    private val promptManager: PromptManager,
) {
    fun findBestPracticesFor(topic: String): String {
        logger.info("Researcher: Searching for best practices regarding '$topic'.")
        val searchResult = adapter.execute(AbstractCommand.PerformWebSearch(topic))
        val prompt = promptManager.getPrompt(
            "researcher.findBestPractices",
            mapOf("searchResults" to searchResult.output, "topic" to topic)
        )
        return ai.executeFlashPrompt(prompt)
    }
}