package com.hereliesaz.geministrator.ui.ide

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.apis.GeminiApiClient
import com.hereliesaz.geministrator.data.SettingsRepository
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

class SearchViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()
    private var geminiApiClient: GeminiApiClient? = null

    init {
        viewModelScope.launch {
            val geminiApiKey = settingsRepository.geminiApiKey.first()
            if (!geminiApiKey.isNullOrBlank()) {
                geminiApiClient = GeminiApiClient(geminiApiKey)
            }
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
                _uiState.update { it.copy(searchResults = listOf(response), isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
