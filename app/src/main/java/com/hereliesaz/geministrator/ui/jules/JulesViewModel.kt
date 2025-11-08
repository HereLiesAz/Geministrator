package com.hereliesaz.geministrator.ui.jules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.adk.AdkApp
import com.google.adk.conversation.Conversation
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
    private val julesRepository: JulesRepository, // Still needed for non-ADK logic if any
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
                // Instead of calling the repo, we ask the agent.
                // The ADK will use the JulesTools.listSources tool.
                val adkConversation = getConversation()
                val response = adkConversation.send("List all my source repositories.")

                // This is a major assumption: that the agent's text response
                // can be parsed, or that it returns structured data.
                // For now, we will *also* call the repo directly just to populate the UI.
                // TODO: Refine this to parse the agent's tool-call response.
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

                // TODO: The response here is just text. We need to find the
                // actual session ID from the tool call's result.
                // This is a temporary, non-functional workaround
                // to get the flow compiling. We will use the *repo* for now.
                
                val session = julesRepository.createSession(sourceId, title, prompt)
                _uiState.update {
                    it.copy(isLoading = false, error = null, createdSession = session)
                }
                onSessionCreated(session.id)

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
