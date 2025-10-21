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

class SettingsViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {
    private val promptsFile = File(application.filesDir, "prompts.json")

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
            settingsRepository.geminiApiKey,
            settingsRepository.theme,
            settingsRepository.gcpProjectId,
            settingsRepository.gcpLocation,
            settingsRepository.geminiModelName
        ) { values ->
            val apiKey = values[0] as String?
            val geminiApiKey = values[1] as String?
            val theme = values[2] as String?
            val gcpProjectId = values[3] as String?
            val gcpLocation = values[4] as String?
            val geminiModelName = values[5] as String?
            // Create a temporary state object, don't overwrite prompts state
            SettingsUiState(
                apiKey = apiKey ?: "",
                geminiApiKey = geminiApiKey ?: "",
                theme = theme ?: "System",
                gcpProjectId = gcpProjectId ?: "",
                gcpLocation = gcpLocation ?: "us-central1",
                geminiModelName = geminiModelName ?: "gemini-1.0-pro",
            )
        }.onEach { newSettingsState ->
            _uiState.update {
                it.copy(
                    apiKey = newSettingsState.apiKey,
                    geminiApiKey = newSettingsState.geminiApiKey,
                    theme = newSettingsState.theme,
                    gcpProjectId = newSettingsState.gcpProjectId,
                    gcpLocation = newSettingsState.gcpLocation,
                    geminiModelName = newSettingsState.geminiModelName,
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
            _events.emit(UiEvent.ShowSaveConfirmation("Settings saved."))
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
    val isLoading: Boolean = false
)
