package com.hereliesaz.geministrator.ui.jules

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.android.data.AndroidConfigStorage
import com.jules.apiclient.JulesApiClient
import com.jules.apiclient.Session
import com.jules.apiclient.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JulesUiState(
    val sources: List<Source> = emptyList(),
    val selectedSource: Source? = null,
    val showCreateSessionDialog: Boolean = false,
    val createdSession: Session? = null,
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
        val client = apiClient ?: return
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

    fun onSourceSelected(source: Source) {
        _uiState.update { it.copy(selectedSource = source, showCreateSessionDialog = true) }
    }

    fun dismissCreateSessionDialog() {
        _uiState.update { it.copy(showCreateSessionDialog = false) }
    }

    fun createSession(title: String, prompt: String) {
        val client = apiClient ?: return
        val source = _uiState.value.selectedSource ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showCreateSessionDialog = false) }
            try {
                val session = client.createSession(prompt, source, title)
                _uiState.update { it.copy(createdSession = session, isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}