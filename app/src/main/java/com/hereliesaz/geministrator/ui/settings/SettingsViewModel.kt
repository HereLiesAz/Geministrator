package com.hereliesaz.geministrator.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.data.AndroidConfigStorage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val config = AndroidConfigStorage(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    init {
        loadSettings()
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

    fun saveSettings() {
        viewModelScope.launch {
            config.saveApiKey(_uiState.value.apiKey)
            config.saveThemePreference(_uiState.value.theme)
            _events.emit(UiEvent.ShowSaveConfirmation)
        }
    }

    sealed class UiEvent {
        data object ShowSaveConfirmation : UiEvent()
    }
}

data class SettingsUiState(
    val apiKey: String = "",
    val theme: String = "System",
)