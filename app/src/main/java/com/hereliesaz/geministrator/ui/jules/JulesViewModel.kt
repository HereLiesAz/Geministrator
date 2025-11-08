package com.hereliesaz.geministrator.ui.jules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.adk.AdkApp
import com.google.adk.conversation.Conversation
import com.google.adk.tool.ToolCall
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
    private val julesRepository: JulesRepository, // Keep for direct, non-agent calls
    private val adkApp: AdkApp // Inject the ADK
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
                "You are an assistant that manages Jules API resources. " +
                        "You will help list sources and create sessions."
            )
        }
        return conversation!!
    }

    fun loadSources() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                // For simple read operations like this, calling the repo
                // directly is acceptable and more efficient than parsing
                // an agent response.
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
                // Send the creation request to the ADK.
                // The ADK will use the JulesTools.createSession tool.
                val adkConversation = getConversation()
                val agentPrompt = """
                    Create a new session for source ID "$sourceId"
                    with the title "$title"
                    and the initial prompt: "$prompt"
                """.trimIndent()
                
                val response = adkConversation.send(agentPrompt)

                // *** THIS IS THE FIX ***
                // Find the result of the `createSession` tool call
                val toolResult = response.messages.lastOrNull { it is ToolResult } as? ToolResult
                val session = toolResult?.result as? Session

                if (session != null) {
                    _uiState.update {
                        it.copy(isLoading = false, error = null, createdSession = session)
                    }
                    // Pass the real session ID from the tool call
                    onSessionCreated(session.id)
                } else {
                    // The agent failed to call the tool or the tool failed
                    val error = "Agent failed to create session. Response: ${response.text}"
                    _uiState.update { it.copy(isLoading = false, error = error) }
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
