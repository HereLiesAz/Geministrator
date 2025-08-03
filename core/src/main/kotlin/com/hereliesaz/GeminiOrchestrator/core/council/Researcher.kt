package com.hereliesaz.GeminiOrchestrator.core.council

import com.hereliesaz.geminiorchestrator.common.AbstractCommand
import com.hereliesaz.geminiorchestrator.common.ExecutionAdapter
import com.hereliesaz.geminiorchestrator.core.GeminiService
import com.hereliesaz.geminiorchestrator.core.council.ILogger

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