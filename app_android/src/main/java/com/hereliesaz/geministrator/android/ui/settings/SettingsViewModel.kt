package com.hereliesaz.geministrator.android.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.android.data.AndroidConfigStorage
import com.hereliesaz.geministrator.common.PromptManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val config = AndroidConfigStorage(application)
    private val promptManager = PromptManager(File(application.cacheDir, "gemini_config"))

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSettings()
        loadPrompts()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    apiKey = config.loadApiKey() ?: "",
                    theme = config.loadThemePreference() ?: "System"
                )
            }
        }
    }

    fun onApiKeyChange(newKey: String) {
        _uiState.update { it.copy(apiKey = newKey) }
    }

    fun onThemeChange(newTheme: String) {
        _uiState.update { it.copy(theme = newTheme) }
    }

    fun onPromptsChange(newPrompts: String) {
        _uiState.update { it.copy(promptsJsonString = newPrompts, promptsDirty = true) }
    }

    private fun loadPrompts() {
        viewModelScope.launch(Dispatchers.IO) {
            val prompts = promptManager.getPromptsAsString()
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(promptsJsonString = prompts, promptsDirty = false) }
            }
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            config.saveApiKey(_uiState.value.apiKey)
            config.saveThemePreference(_uiState.value.theme)
        }
    }

    fun savePrompts() {
        if (!_uiState.value.promptsDirty) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                promptManager.savePromptsFromString(_uiState.value.promptsJsonString)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(promptsDirty = false) }
                }
            } catch (e: Exception) {
                // TODO: Propagate error to UI
            }
        }
    }

    fun resetPrompts() {
        viewModelScope.launch(Dispatchers.IO) {
            if (promptManager.resetToDefaults()) {
                loadPrompts()
            }
        }
    }
}

data class SettingsUiState(
    val apiKey: String = "",
    val theme: String = "System",
    val promptsJsonString: String = "Loading...",
    val promptsDirty: Boolean = false,
)