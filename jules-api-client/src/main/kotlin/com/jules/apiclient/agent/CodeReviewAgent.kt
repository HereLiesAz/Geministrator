package com.jules.apiclient.agent

import com.google.adk.agents.LlmAgent

fun createCodeReviewAgent(modelName: String, tools: GitHubTools) = LlmAgent.builder()
    .model(modelName)
    .name("geministrator_code_reviewer")
    .instruction("You are a code reviewer. Your job is to review code diffs and provide feedback.")
    .description("Reviews code diffs and provides feedback.")
    .withTools(listOf(tools))
    .build()

private fun Any.build(): Any {
    TODO("Not yet implemented")
}

private fun LlmAgent.Builder.withTools(listOf: List<GitHubTools>): Any {
    TODO("Not yet implemented")
}
