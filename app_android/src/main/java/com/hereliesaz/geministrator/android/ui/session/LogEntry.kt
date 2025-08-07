package com.hereliesaz.geministrator.android.ui.session

import com.hereliesaz.geministrator.android.ui.theme.Agent
import java.util.UUID

data class LogEntry(
    val message: String,
    val agent: Agent,
    val content: String? = null, // For holding markdown or code
    val isError: Boolean = false,
    val isWorking: Boolean = false, // To show a placeholder
    val isAwaitingInput: Boolean = false, // To show a prompt for user
    val id: String = UUID.randomUUID().toString(), // For stable LazyColumn keys
)