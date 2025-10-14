package com.hereliesaz.geministrator.ui.ide

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.cloud.vertexai.generativeai.ResponseHandler
import com.hereliesaz.geministrator.data.SettingsRepository
import com.jules.apiclient.GeminiApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val searchQuery: String = "",
    val searchResults: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private var geminiApiClient: GeminiApiClient? = null

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val githubRepository = settingsRepository.githubRepository.first()
            val gcpLocation = settingsRepository.gcpLocation.first()
            val geminiModelName = settingsRepository.geminiModelName.first()

            if (githubRepository.isNullOrBlank() || gcpLocation.isNullOrBlank() || geminiModelName.isNullOrBlank()) {
                _uiState.update { it.copy(error = "Gemini settings not found. Please set them in Settings.") }
                return@launch
            }

            geminiApiClient = GeminiApiClient(
                projectId = githubRepository,
                location = gcpLocation,
                modelName = geminiModelName
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun performSearch() {
        val client = geminiApiClient ?: return
        val query = _uiState.value.searchQuery
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // This is a placeholder for the actual search logic.
                // We will need to find a way to provide the codebase as context to the Gemini API.
                val response = client.generateContent("Find code related to: $query in the project.")
                val textResponse = response?.let { ResponseHandler.getText(it) } ?: ""
                _uiState.update { it.copy(searchResults = listOf(textResponse), isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
