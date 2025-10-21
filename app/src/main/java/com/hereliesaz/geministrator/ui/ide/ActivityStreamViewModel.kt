package com.hereliesaz.geministrator.ui.ide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.data.SettingsRepository
import com.jules.apiclient.JulesApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ActivityStreamViewModel(
    private var julesApiClient: JulesApiClient?,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(IdeUiState())
    val uiState = _uiState.asStateFlow()

    fun loadActivities(sessionId: String) {
        val client = julesApiClient ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val activities = client.getActivities(sessionId).activities
                // TODO: Update the UI with the activities
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
