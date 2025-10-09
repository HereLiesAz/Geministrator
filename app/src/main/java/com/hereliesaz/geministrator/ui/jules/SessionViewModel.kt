package com.hereliesaz.geministrator.ui.jules

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.android.data.AndroidConfigStorage
import com.jules.apiclient.Activity
import com.jules.apiclient.JulesApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionUiState(
    val activities: List<Activity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class SessionViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val sessionId: String = savedStateHandle.get<String>("sessionId")
        ?: throw IllegalArgumentException("Session ID not found in SavedStateHandle")
    private val config = AndroidConfigStorage(application)
    private var apiClient: JulesApiClient? = null

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val apiKey = config.loadApiKey()
            if (apiKey.isNullOrBlank()) {
                _uiState.update { it.copy(error = "API Key not found. Please set it in Settings.") }
            } else {
                apiClient = JulesApiClient(apiKey)
                loadActivities()
            }
        }
    }

    fun loadActivities() {
        val client = apiClient ?: return
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
        val client = apiClient ?: return
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
}