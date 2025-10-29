package com.hereliesaz.geministrator.ui.jules

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
//import com.hereliesaz.geministrator.apis.GeminiApiClient
import com.hereliesaz.geministrator.data.A2ACommunicator
import com.hereliesaz.geministrator.data.SettingsRepository
import androidx.annotation.VisibleForTesting
import com.jules.apiclient.Activity
import com.jules.apiclient.JulesApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionUiState(
    val sessionId: String = "",
    val activities: List<Activity> = emptyList(),
    val subTasks: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val geminiResponse: String? = null
)

class SessionViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
    private var julesApiClient: JulesApiClient?,
//    private val geminiApiClient: GeminiApiClient?,
    private val a2aCommunicator: A2ACommunicator?
) : ViewModel() {

    internal val sessionId: String = savedStateHandle.get<String>("sessionId")!!
    private val roles: Set<String> = savedStateHandle.get<String>("roles")?.split(",")?.toSet() ?: emptySet()

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState = _uiState.asStateFlow()

    fun loadActivities() {
        val client = julesApiClient ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val activities = client.getActivities(sessionId).activities
                _uiState.update { it.copy(activities = activities, isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun sendMessage(prompt: String) {
//        if (prompt.startsWith("/gemini")) {
//            val geminiPrompt = prompt.substringAfter("/gemini").trim()
//            a2aCommunicator?.sendMessage(sessionId, geminiPrompt) { response ->
//                _uiState.update { it.copy(geminiResponse = response) }
//            }
//        } else {
            val client = julesApiClient ?: return
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                try {
                    client.sendMessage(sessionId, prompt)
                    // After sending, reload the activities to see the agent's response.
                    val activities = client.getActivities(sessionId).activities
                    _uiState.update { it.copy(activities = activities, isLoading = false, error = null) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            }
//        }
    }

    fun decomposeTask(task: String) {
//        val client = geminiApiClient ?: return
        viewModelScope.launch {
            if (!roles.contains("planner")) {
                _uiState.update { it.copy(error = "The 'planner' role is not enabled for this session.") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            try {
//                val prompt = "Decompose the following high-level task into a list of smaller, manageable sub-tasks:\n\n$task"
//                val response = client.generateContent(prompt)
//                val subTasks = response.split("\n").filter { it.isNotBlank() }
//                _uiState.update { it.copy(subTasks = subTasks, isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
