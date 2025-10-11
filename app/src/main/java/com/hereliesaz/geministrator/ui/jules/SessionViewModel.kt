package com.hereliesaz.geministrator.ui.jules

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.data.SettingsRepository
import com.jules.apiclient.A2ACommunicator
import com.jules.apiclient.Activity
import com.jules.apiclient.GeminiApiClient
import com.jules.apiclient.JulesApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionUiState(
    val activities: List<Activity> = emptyList(),
    val subTasks: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val geminiResponse: String? = null
)

class SessionViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val sessionId: String = savedStateHandle.get<String>("sessionId")
        ?: throw IllegalArgumentException("Session ID not found in SavedStateHandle")
    private val roles: Set<String> = savedStateHandle.get<String>("roles")?.split(",").orEmpty().toSet()
    private val settingsRepository = SettingsRepository(application)
    private var julesApiClient: JulesApiClient? = null
    private var geminiApiClient: GeminiApiClient? = null
    private var a2aCommunicator: A2ACommunicator? = null

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val apiKey = settingsRepository.apiKey.first()
            val gcpProjectId = settingsRepository.gcpProjectId.first()
            val gcpLocation = settingsRepository.gcpLocation.first()
            val geminiModelName = settingsRepository.geminiModelName.first()

            if (apiKey.isNullOrBlank()) {
                _uiState.update { it.copy(error = "API Key not found. Please set it in Settings.") }
                return@launch
            }
            if (gcpProjectId.isNullOrBlank() || gcpLocation.isNullOrBlank() || geminiModelName.isNullOrBlank()) {
                _uiState.update { it.copy(error = "Gemini settings not found. Please set them in Settings.") }
                return@launch
            }

            julesApiClient = JulesApiClient(apiKey)
            geminiApiClient = GeminiApiClient(
                projectId = gcpProjectId,
                location = gcpLocation,
                modelName = geminiModelName
            )
            a2aCommunicator = A2ACommunicator(julesApiClient!!, geminiApiClient!!)
            loadActivities()
        }
    }

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
    }

    fun askGemini(prompt: String) {
        val communicator = a2aCommunicator ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = communicator.julesToGemini(prompt)
                _uiState.update { it.copy(isLoading = false, geminiResponse = response) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun decomposeTask(task: String) {
        val client = geminiApiClient ?: return
        viewModelScope.launch {
            if (!roles.contains("planner")) {
                _uiState.update { it.copy(error = "The 'planner' role is not enabled for this session.") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            try {
                val prompt = "Decompose the following high-level task into a list of smaller, manageable sub-tasks:\n\n$task"
                val response = client.generateContent(prompt)
                val subTasks = com.google.cloud.vertexai.generativeai.ResponseHandler.getText(response).split("\n").filter { it.isNotBlank() }
                _uiState.update { it.copy(subTasks = subTasks, isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
