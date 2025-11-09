package com.hereliesaz.geministrator.ui.jules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.data.JulesRepository
import com.jules.apiclient.Session
import com.jules.apiclient.Source
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JulesUiState(
    val sources: List<Source> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdSession: Session? = null
)

@HiltViewModel
class JulesViewModel @Inject constructor(
    private val julesRepository: JulesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(JulesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSources()
    }

    fun loadSources() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                // For a simple read, calling the repo directly is fine.
                val sources = julesRepository.getSources()
                _uiState.update {
                    it.copy(sources = sources, isLoading = false, error = null)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun createSession(sourceId: String, title: String, prompt: String, onSessionCreated: (String) -> Unit) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                // Find the source from the list of sources
                val source = _uiState.value.sources.find { it.id == sourceId }
                if (source == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Source not found") }
                    return@launch
                }
                val session = julesRepository.createSession(prompt, source, title, "")
                _uiState.update {
                    it.copy(isLoading = false, error = null, createdSession = session)
                }
                onSessionCreated(session.id)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}