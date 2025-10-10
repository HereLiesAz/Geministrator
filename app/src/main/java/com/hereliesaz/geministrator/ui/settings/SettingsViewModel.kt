package com.hereliesaz.geministrator.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val promptsFile = File(application.filesDir, "prompts.json")

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    init {
        loadSettings()
        loadPrompts()
    }

    private fun loadSettings() {
        // Helper data class for type-safe combination of 5 flows
        data class CombinedSettings(
            val apiKey: String?,
            val theme: String?,
            val gcpProjectId: String?,
            val gcpLocation: String?,
            val geminiModelName: String?
        )

        combine(
            settingsRepository.apiKey,
            settingsRepository.theme,
            settingsRepository.gcpProjectId,
            settingsRepository.gcpLocation,
            settingsRepository.geminiModelName
        ) { apiKey, theme, gcpProjectId, gcpLocation, geminiModelName ->
            CombinedSettings(apiKey, theme, gcpProjectId, gcpLocation, geminiModelName)
        }.combine(settingsRepository.geminiApiKey) { combined, geminiApiKey ->
            SettingsUiState(
                apiKey = combined.apiKey ?: "",
                geminiApiKey = geminiApiKey ?: "",
                theme = combined.theme ?: "System",
                gcpProjectId = combined.gcpProjectId ?: "",
                gcpLocation = combined.gcpLocation ?: "us-central1",
                geminiModelName = combined.geminiModelName ?: "gemini-1.0-pro"
            )
        }.onEach { newSettingsState ->
            _uiState.update {
                it.copy(
                    apiKey = newSettingsState.apiKey,
                    geminiApiKey = newSettingsState.geminiApiKey,
                    theme = newSettingsState.theme,
                    gcpProjectId = newSettingsState.gcpProjectId,
                    gcpLocation = newSettingsState.gcpLocation,
                    geminiModelName = newSettingsState.geminiModelName
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun loadPrompts() {
        viewModelScope.launch {
            val promptsJson = if (promptsFile.exists()) {
                promptsFile.readText()
            } else {
                getApplication<Application>().assets.open("prompts.json").bufferedReader().use { it.readText() }
            }
            _uiState.update { it.copy(promptsJsonString = promptsJson, promptsDirty = false) }
        }
    }

    fun onApiKeyChange(newKey: String) {
        _uiState.update { it.copy(apiKey = newKey) }
    }

    fun onGeminiApiKeyChange(newKey: String) {
        _uiState.update { it.copy(geminiApiKey = newKey) }
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
            settingsRepository.saveGeminiApiKey(_uiState.value.geminiApiKey)
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
        viewModelScope.launch {
            if (promptsFile.exists()) {
                promptsFile.delete()
            }
            loadPrompts()
        }
    }

    fun savePrompts() {
        viewModelScope.launch {
            promptsFile.writeText(_uiState.value.promptsJsonString)
            _uiState.update { it.copy(promptsDirty = false) }
        }
    }

    sealed class UiEvent {
        data object ShowSaveConfirmation : UiEvent()
    }
}

data class SettingsUiState(
    val apiKey: String = "",
    val geminiApiKey: String = "",
    val theme: String = "System",
    val gcpProjectId: String = "",
    val gcpLocation: String = "us-central1",
    val geminiModelName: String = "gemini-1.0-pro",
    val promptsJsonString: String = "",
    val promptsDirty: Boolean = false,
)