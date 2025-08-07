package com.hereliesaz.geministrator.android.ui.navigation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SessionHistory(
    val id: String,
    val name: String,
    val timestamp: String
)

class HistoryViewModel : ViewModel() {
    private val _sessions = MutableStateFlow<List<SessionHistory>>(emptyList())
    val sessions = _sessions.asStateFlow()

    init {
        // Dummy data for now
        _sessions.value = listOf(
            SessionHistory("1", "Fix a bug", "2025-08-06"),
            SessionHistory("2", "Implement a feature", "2025-08-05"),
            SessionHistory("3", "Refactor the UI", "2025-08-04")
        )
    }
}
