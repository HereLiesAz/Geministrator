package com.jules.apiclient.agent

import com.google.adk.agents.SequentialAgent

fun createCodeReviewAgent(modelName: String): SequentialAgent {
    val generator = createGeneratorAgent(modelName)
    val critic = createCriticAgent(modelName)

    return SequentialAgent.builder()
        .name("CodeReviewAgent")
        .subAgents(generator, critic)
        .build()
}
