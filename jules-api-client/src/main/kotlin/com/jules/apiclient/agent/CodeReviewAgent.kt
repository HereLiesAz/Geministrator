package com.jules.apiclient.agent

import com.google.adk.agents.LlmAgent
import com.github.apiclient.GitHubApiClient
import com.jules.apiclient.agent.GitHubTools

fun createCodeReviewAgent(modelName: String, gitHubApiClient: GitHubApiClient) = LlmAgent.builder()
    .model(modelName)
    .name("geministrator_code_reviewer")
    .instruction("You are a code reviewer. Your job is to review code diffs and provide feedback.")
    .description("Reviews code diffs and provides feedback.")
    .addTool(GitHubTools(gitHubApiClient))
    .build()
