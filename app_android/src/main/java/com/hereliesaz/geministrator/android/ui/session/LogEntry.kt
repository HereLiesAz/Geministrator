package com.hereliesaz.geministrator.android.ui.session

import com.hereliesaz.geministrator.android.ui.theme.Agent

data class LogEntry(
    val message: String,
    val agent: Agent,
    val content: String? = null, // For holding markdown or code
    val isWorking: Boolean = false // To show a placeholder
)