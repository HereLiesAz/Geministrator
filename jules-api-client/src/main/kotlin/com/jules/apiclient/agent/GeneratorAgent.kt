package com.jules.apiclient.agent

import com.google.adk.agents.LlmAgent

fun createGeneratorAgent(modelName: String) = LlmAgent.builder()
    .name("Generator")
    .model(modelName)
    .instruction("You are a code reviewer. Your job is to review code diffs and provide feedback.")
    .outputKey("draft_text")
    .build()
