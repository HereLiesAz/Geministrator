package com.hereliesaz.geministrator.ui.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.data.SessionRepository
import com.jules.apiclient.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionUiState(
    val session: Session? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState = _uiState.asStateFlow()

    private val sessionId: String = savedStateHandle.get<String>("sessionId")!!

    init {
        loadSession()
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionRepository.session.collect {
                _uiState.update { state -> state.copy(session = it) }
            }
        }
    }

    private fun loadSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                sessionRepository.loadSession(sessionId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun sendMessage(prompt: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                sessionRepository.sendMessage(prompt)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
