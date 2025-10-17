package com.jules.apiclient.agent

import com.google.adk.agents.LlmAgent

fun createCodeReviewAgent(modelName: String, toolSet: GitHubToolSet) = LlmAgent.builder()
    .model(modelName)
    .name("geministrator_code_reviewer")
    .instruction("You are a code reviewer. Your job is to review code diffs and provide feedback.")
    .description("Reviews code diffs and provides feedback.")
    .tools(toolSet.getPullRequests, toolSet.getPullRequestDiff, toolSet.createComment)
    .build()
