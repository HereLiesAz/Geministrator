package com.hereliesaz.geministrator.ui.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.adk.AdkApp
import com.google.adk.conversation.Conversation
import com.hereliesaz.geministrator.data.HistoryRepository
import com.hereliesaz.geministrator.data.Prompt
import com.hereliesaz.geministrator.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val adkApp: AdkApp, // The ADK is now injected
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    // The ADK conversation is stateful and held by the ViewModel
    private var conversation: Conversation? = null

    init {
        loadHistory()
    }

    private fun getConversation(): Conversation {
        if (conversation == null) {
            // Create a new conversation with a default system prompt
            conversation = adkApp.startConversation(
                "You are Geministrator, a mobile AI assistant. " +
                        "You can use tools to interact with Jules and GitHub."
            )
        }
        return conversation!!
    }

    fun loadHistory() {
        viewModelScope.launch {
            val history = historyRepository.getHistory()
            _uiState.update {
                it.copy(
                    history = history,
                    // TODO: We may need to re-hydrate the ADK conversation here
                )
            }
        }
    }

    fun sendMessage(text: String) {
        val userPrompt = Prompt(
            id = UUID.randomUUID().toString(),
            text = text,
            isFromUser = true
        )

        // Add user prompt to history immediately
        viewModelScope.launch {
            historyRepository.addPrompt(userPrompt)
            _uiState.update {
                it.copy(history = it.history + userPrompt, isLoading = true)
            }
        }

        // Send to ADK for processing
        viewModelScope.launch {
            try {
                val adkConversation = getConversation()
                val response = adkConversation.send(text)

                // Process and save the agent's response
                val agentResponse = Prompt(
                    id = UUID.randomUUID().toString(),
                    text = response.text, // The final text from the agent
                    isFromUser = false,
                    // TODO: We could parse tool calls from response.messages
                    // and display them in a structured way.
                )
                historyRepository.addPrompt(agentResponse)

                // Update UI with the final response
                _uiState.update {
                    it.copy(history = it.history + agentResponse, isLoading = false)
                }

            } catch (e: Exception) {
                // Handle errors
                val errorResponse = Prompt(
                    id = UUID.randomUUID().toString(),
                    text = "Error: ${e.message}",
                    isFromUser = false
                )
                historyRepository.addPrompt(errorResponse)
                _uiState.update {
                    it.copy(history = it.history + errorResponse, isLoading = false)
                }
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
            conversation = null // Start a new conversation
            _uiState.update { it.copy(history = emptyList()) }
        }
    }
}
