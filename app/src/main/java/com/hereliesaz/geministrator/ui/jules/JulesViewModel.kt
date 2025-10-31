package com.hereliesaz.geministrator.ui.jules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.data.JulesRepository
import com.hereliesaz.geministrator.data.Prompt
import com.hereliesaz.geministrator.data.PromptsRepository
import com.jules.apiclient.Session
import com.jules.apiclient.Source
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class JulesUiState(
    val sources: List<Source> = emptyList(),
    val selectedSource: Source? = null,
    val showCreateSessionDialog: Boolean = false,
    val createdSession: Session? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val roles: List<Prompt> = emptyList(),
    val selectedRoles: Set<String> = emptySet()
)

@HiltViewModel
class JulesViewModel @Inject constructor(
    private val julesRepository: JulesRepository,
    private val promptsRepository: PromptsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(JulesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSources()
        loadRoles()
    }

    private fun loadRoles() {
        viewModelScope.launch {
            try {
                val roles = promptsRepository.getPrompts()
                _uiState.update { it.copy(roles = roles) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onRoleSelected(roleName: String, isSelected: Boolean) {
        val selectedRoles = _uiState.value.selectedRoles.toMutableSet()
        if (isSelected) {
            selectedRoles.add(roleName)
        } else {
            selectedRoles.remove(roleName)
        }
        _uiState.update { it.copy(selectedRoles = selectedRoles) }
    }

    fun loadSources() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val sources = julesRepository.getSources()
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
        val source = _uiState.value.selectedSource ?: return
        val selectedRoles = _uiState.value.selectedRoles

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showCreateSessionDialog = false) }
            try {
                val session = julesRepository.createSession(prompt, source, title, Json.encodeToString(selectedRoles))
                _uiState.update { it.copy(createdSession = session, isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
