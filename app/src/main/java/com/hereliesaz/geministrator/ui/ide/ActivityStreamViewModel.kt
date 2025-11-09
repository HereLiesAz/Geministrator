package com.hereliesaz.geministrator.ui.ide

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.data.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.hereliesaz.geministrator.data.Message
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ActivityStreamUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class ActivityStreamViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val sessionId: UUID = UUID.fromString(savedStateHandle.get<String>("sessionId")!!)

    private val _uiState = MutableStateFlow(ActivityStreamUiState())
    val uiState = _uiState.asStateFlow()

    fun loadSessionHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            historyRepository.getSessionHistory(sessionId)
                .collect { messages ->
                    _uiState.update {
                        it.copy(
                            messages = messages.map { Message(it.id, it.text, it.timestamp) },
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun onMessageSent(message: String) {
        viewModelScope.launch {
            historyRepository.addMessageToHistory(sessionId, message)
        }
    }
}
