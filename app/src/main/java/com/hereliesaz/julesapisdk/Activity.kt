package com.hereliesaz.julesapisdk

sealed class Activity {
    data class UserMessageActivity(val id: String, val prompt: String) : Activity()
    data class AgentResponseActivity(val id: String, val response: String) : Activity()
    data class ToolCallActivity(val id: String, val toolName: String, val args: String) : Activity()
    data class ToolOutputActivity(val id: String, val toolName: String, val output: String) : Activity()
    data class PlanActivity(val id: String, val plan: String) : Activity()
}
