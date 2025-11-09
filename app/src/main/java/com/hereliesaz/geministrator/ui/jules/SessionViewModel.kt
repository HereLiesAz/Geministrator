package com.hereliesaz.geministrator.ui.jules

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.hereliesaz.geministrator.data.A2ACommunicator
import com.hereliesaz.geministrator.data.JulesRepository
import com.jules.apiclient.Activity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionUiState(
    val sessionId: String = "",
    val activities: List<Activity> = emptyList(),
    val subTasks: List<String> = emptyList(),
    val geminiResponse: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val julesRepository: JulesRepository,
    private val geminiModel: GenerativeModel,
    private val a2aCommunicator: A2ACommunicator,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    internal val sessionId: String = savedStateHandle.get<String>("sessionId")!!
    private val roles: Set<String> = savedStateHandle.get<String>("roles")?.split(",")?.toSet() ?: emptySet()

    private val _uiState = MutableStateFlow(SessionUiState(sessionId = sessionId))
    val uiState = _uiState.asStateFlow()

    init {
        loadActivities()
    }

    fun loadActivities() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val activitiesResponse = julesRepository.getActivities(sessionId)
                _uiState.update {
                    it.copy(
                        activities = activitiesResponse.activities,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun sendMessage(prompt: String) {
        if (prompt.startsWith("/gemini")) {
            // This is an A2A (ADK) command
            _uiState.update { it.copy(isLoading = true) }
            val geminiPrompt = prompt.substringAfter("/gemini").trim()
            a2aCommunicator.sendMessage(sessionId, geminiPrompt) { response ->
                _uiState.update {
                    it.copy(geminiResponse = response, isLoading = false)
                }
            }
        } else {
            // This is a standard Jules API command
            _uiState.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                try {
                    julesRepository.sendMessage(sessionId, prompt)
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                } finally {
                    loadActivities() // Refresh after sending
                }
            }
        }
    }

    fun decomposeTask(task: String) {
        viewModelScope.launch {
            if (!roles.contains("planner")) {
                _uiState.update { it.copy(error = "The 'planner' role is not enabled for this session.") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            try {
                val prompt = "Decompose the following high-level task into a list of smaller, manageable sub-tasks:\n\n$task"
                val response = geminiModel.generateContent(prompt)
                val subTasks = response.text?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
                _uiState.update { it.copy(subTasks = subTasks, isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}