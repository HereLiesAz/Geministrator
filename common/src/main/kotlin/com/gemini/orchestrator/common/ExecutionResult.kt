package com.gemini.orchestrator.common

data class ExecutionResult(
    val isSuccess: Boolean,
    val output: String,
    val data: Any? = null
)
