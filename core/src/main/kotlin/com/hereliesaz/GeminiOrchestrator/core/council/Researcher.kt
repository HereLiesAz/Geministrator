package com.hereliesaz.GeminiOrchestrator.core.council

import com.hereliesaz.GeminiOrchestrator.common.AbstractCommand
import com.hereliesaz.GeminiOrchestrator.common.ExecutionAdapter
import com.hereliesaz.GeminiOrchestrator.core.GeminiService

class Researcher(private val logger: ILogger, private val ai: GeminiService, private val adapter: ExecutionAdapter) {
    fun findBestPracticesFor(topic: String): String {
        logger.log("📚 Researcher: Searching for best practices regarding '$topic'.")
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