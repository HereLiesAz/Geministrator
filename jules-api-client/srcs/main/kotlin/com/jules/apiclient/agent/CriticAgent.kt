package com.jules.apiclient.agent

import com.google.adk.agents.LlmAgent

fun createCriticAgent(modelName: String) = LlmAgent.builder()
    .name("Critic")
    .model(modelName)
    .instruction("You are a code reviewer. Your job is to critique the code review provided in {draft_text} and provide feedback.")
    .outputKey("review_status")
    .build()
