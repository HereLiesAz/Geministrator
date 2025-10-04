package com.hereliesaz.geministrator.android.ui.jules

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.android.data.AndroidConfigStorage
import com.jules.apiclient.JulesApiClient
import com.jules.apiclient.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JulesUiState(
    val sources: List<Source> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class JulesViewModel(application: Application) : AndroidViewModel(application) {

    private val config = AndroidConfigStorage(application)
    private var apiClient: JulesApiClient? = null

    private val _uiState = MutableStateFlow(JulesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val apiKey = config.loadApiKey()
            if (apiKey.isNullOrBlank()) {
                _uiState.update { it.copy(error = "API Key not found. Please set it in Settings.") }
            } else {
                apiClient = JulesApiClient(apiKey)
                loadSources()
            }
        }
    }

    fun loadSources() {
        val client = apiClient
        if (client == null) {
            _uiState.update { it.copy(isLoading = false, error = "API Key not found. Please set it in Settings.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val sources = client.getSources().sources
                _uiState.update { it.copy(sources = sources, isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}