package com.hereliesaz.geministrator.ui.jules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.adk.AdkApp
import com.google.adk.conversation.Conversation
import com.google.adk.tool.ToolResult
import com.hereliesaz.geministrator.data.JulesRepository
import com.jules.apiclient.Session
import com.jules.apiclient.Source
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JulesUiState(
    val sources: List<Source> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdSession: Session? = null
)

@HiltViewModel
class JulesViewModel @Inject constructor(
    private val julesRepository: JulesRepository, // For direct reads
    private val adkApp: AdkApp
) : ViewModel() {

    private val _uiState = MutableStateFlow(JulesUiState())
    val uiState = _uiState.asStateFlow()

    private var conversation: Conversation? = null

    init {
        loadSources()
    }

    private fun getConversation(): Conversation {
        if (conversation == null) {
            conversation = adkApp.startConversation(
                "You are an assistant that manages Jules API resources."
            )
        }
        return conversation!!
    }

    fun loadSources() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                // For a simple read, calling the repo directly is fine.
                val sources = julesRepository.getSources()
                _uiState.update {
                    it.copy(sources = sources, isLoading = false, error = null)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun createSession(sourceId: String, title: String, prompt: String, onSessionCreated: (String) -> Unit) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val adkConversation = getConversation()
                val agentPrompt = """
                    Create a new session for source ID "$sourceId"
                    with the title "$title"
                    and the initial prompt: "$prompt"
                """.trimIndent()

                val response = adkConversation.send(agentPrompt)

                val toolResult = response.messages.lastOrNull { it is ToolResult } as? ToolResult
                val session = toolResult?.result as? Session

                if (session != null) {
                    _uiState.update {
                        it.copy(isLoading = false, error = null, createdSession = session)
                    }
                    onSessionCreated(session.id)
                } else {
                    val error = "Agent failed to create session. Response: ${response.text}"
                    _uiState.update { it.copy(isLoading = false, error = error) }
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}