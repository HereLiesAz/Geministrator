package com.hereliesaz.geministrator.android.ui.main

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.android.ui.project.ProjectViewModel
import com.hereliesaz.geministrator.android.ui.session.Session
import com.hereliesaz.geministrator.android.ui.session.SessionViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    fun startSession(prompt: String, projectViewModel: ProjectViewModel) {
        val projectUri = projectViewModel.uiState.value.projectUri ?: return

        val sessionViewModel = SessionViewModel(prompt, projectViewModel)

        val nextId = (_uiState.value.sessions.maxOfOrNull { it.id } ?: 0) + 1
        val title = prompt.take(20).let { if (it.length == 20) "$it..." else it }
        val newSession = Session(
            id = nextId,
            title = title,
            initialPrompt = prompt,
            projectUri = projectUri,
            viewModel = sessionViewModel
        )
        _uiState.update {
            it.copy(
                sessions = it.sessions + newSession,
                selectedSessionIndex = it.sessions.size,
                showNewSessionDialog = false
            )
        }
    }

    fun onShowNewSessionDialog() {
        _uiState.update { it.copy(showNewSessionDialog = true) }
    }

    fun onDismissNewSessionDialog() {
        _uiState.update { it.copy(showNewSessionDialog = false) }
    }

    fun selectSession(index: Int) {
        if (index in _uiState.value.sessions.indices) {
            _uiState.update { it.copy(selectedSessionIndex = index) }
        }
    }
}

data class MainUiState(
    val sessions: List<Session> = emptyList(),
    val selectedSessionIndex: Int = 0,
    val showNewSessionDialog: Boolean = false
)