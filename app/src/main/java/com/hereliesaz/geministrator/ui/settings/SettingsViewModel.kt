package com.hereliesaz.geministrator.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.data.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        combine(
            settingsRepository.apiKey,
            settingsRepository.theme,
            settingsRepository.gcpProjectId,
            settingsRepository.gcpLocation,
            settingsRepository.geminiModelName
        ) { apiKey, theme, gcpProjectId, gcpLocation, geminiModelName ->
            SettingsUiState(
                apiKey = apiKey ?: "",
                theme = theme ?: "System",
                gcpProjectId = gcpProjectId ?: "",
                gcpLocation = gcpLocation ?: "us-central1",
                geminiModelName = geminiModelName ?: "gemini-1.0-pro"
            )
        }.onEach { newState ->
            _uiState.update { newState }
        }.launchIn(viewModelScope)
    }

    fun onApiKeyChange(newKey: String) {
        _uiState.update { it.copy(apiKey = newKey) }
    }

    fun onThemeChange(newTheme: String) {
        _uiState.update { it.copy(theme = newTheme) }
    }

    fun onGcpProjectIdChange(newProjectId: String) {
        _uiState.update { it.copy(gcpProjectId = newProjectId) }
    }

    fun onGcpLocationChange(newLocation: String) {
        _uiState.update { it.copy(gcpLocation = newLocation) }
    }

    fun onGeminiModelNameChange(newModelName: String) {
        _uiState.update { it.copy(geminiModelName = newModelName) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            settingsRepository.saveApiKey(_uiState.value.apiKey)
            settingsRepository.saveTheme(_uiState.value.theme)
            settingsRepository.saveGcpProjectId(_uiState.value.gcpProjectId)
            settingsRepository.saveGcpLocation(_uiState.value.gcpLocation)
            settingsRepository.saveGeminiModelName(_uiState.value.geminiModelName)
            _events.emit(UiEvent.ShowSaveConfirmation)
        }
    }

    fun onPromptsChange(newText: String) {
        _uiState.update { it.copy(promptsJsonString = newText, promptsDirty = true) }
    }

    fun resetPrompts() {
        // TODO: Implement resetting from a default source
    }

    fun savePrompts() {
        // TODO: Implement saving to a file
    }

    sealed class UiEvent {
        data object ShowSaveConfirmation : UiEvent()
    }
}

data class SettingsUiState(
    val apiKey: String = "",
    val theme: String = "System",
    val gcpProjectId: String = "",
    val gcpLocation: String = "us-central1",
    val geminiModelName: String = "gemini-1.0-pro",
    val promptsJsonString: String = "",
    val promptsDirty: Boolean = false,
)