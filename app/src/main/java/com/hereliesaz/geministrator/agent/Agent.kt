package com.hereliesaz.geministrator.agent

interface Agent {
    fun execute(request: Request): Response

    data class Request(val prompt: String)
    data class Response(val text: String)
}
