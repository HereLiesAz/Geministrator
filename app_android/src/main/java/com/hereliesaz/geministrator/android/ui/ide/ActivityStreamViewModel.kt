package com.hereliesaz.geministrator.android.ui.ide

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.android.data.AndroidConfigStorage
import com.jules.apiclient.Activity
import com.jules.apiclient.JulesApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ActivityStreamUiState(
    val activities: List<Activity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ActivityStreamViewModel(application: Application) : AndroidViewModel(application) {

    private val config = AndroidConfigStorage(application)
    private var apiClient: JulesApiClient? = null

    private val _uiState = MutableStateFlow(ActivityStreamUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val apiKey = config.loadApiKey()
            if (apiKey.isNullOrBlank()) {
                _uiState.update { it.copy(error = "API Key not found. Please set it in Settings.") }
            } else {
                apiClient = JulesApiClient(apiKey)
            }
        }
    }

    fun loadActivities(sessionId: String) {
        val client = apiClient
        if (client == null) {
            _uiState.update { it.copy(isLoading = false, error = "API Key not found. Please set it in Settings.") }
            return
        }

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
}