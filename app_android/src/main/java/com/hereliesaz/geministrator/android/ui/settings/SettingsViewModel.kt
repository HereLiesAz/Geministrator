package com.hereliesaz.geministrator.android.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.android.data.AndroidConfigStorage
import com.hereliesaz.geministrator.core.config.ConfigStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val config: ConfigStorage = AndroidConfigStorage(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val apiKey = config.loadApiKey() ?: ""
            _uiState.update { it.copy(apiKey = apiKey) }
        }
    }

    fun onApiKeyChange(newKey: String) {
        _uiState.update { it.copy(apiKey = newKey) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            config.saveApiKey(_uiState.value.apiKey)
            // You could add a "Saved!" message to the UI state here
        }
    }
}

data class SettingsUiState(
    val apiKey: String = ""
)