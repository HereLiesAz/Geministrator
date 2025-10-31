package com.hereliesaz.geministrator.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.data.Prompt
import com.hereliesaz.geministrator.data.PromptsRepository
import com.hereliesaz.geministrator.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RolesSettingsUiState(
    val prompts: List<Prompt> = emptyList(),
    val enabledRoles: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RolesSettingsViewModel @Inject constructor(
    private val promptsRepository: PromptsRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RolesSettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadPrompts()
        viewModelScope.launch {
            settingsRepository.enabledRoles.collect { enabledRoles ->
                _uiState.update { it.copy(enabledRoles = enabledRoles) }
            }
        }
    }

    private fun loadPrompts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val prompts = promptsRepository.getPrompts()
                _uiState.update { it.copy(prompts = prompts, isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onRoleEnabledChanged(roleName: String, isEnabled: Boolean) {
        viewModelScope.launch {
            val currentEnabledRoles = _uiState.value.enabledRoles.toMutableSet()
            if (isEnabled) {
                currentEnabledRoles.add(roleName)
            } else {
                currentEnabledRoles.remove(roleName)
            }
            settingsRepository.saveEnabledRoles(currentEnabledRoles)
        }
    }
}
