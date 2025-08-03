package com.hereliesaz.geministrator.common

interface ExecutionAdapter {
    fun execute(command: AbstractCommand): ExecutionResult
}

data class ExecutionResult(
    val isSuccess: Boolean,
    val output: String,
    val data: Any? = null
)