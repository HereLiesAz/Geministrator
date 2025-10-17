package com.hereliesaz.geministrator.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.data.Prompt
import com.hereliesaz.geministrator.data.PromptsRepository
import com.hereliesaz.geministrator.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RolesSettingsUiState(
    val prompts: List<Prompt> = emptyList(),
    val enabledRoles: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class RolesSettingsViewModel(
    private val promptsRepository: PromptsRepository,
    private var settingsRepository: SettingsRepository,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(RolesSettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadPrompts()
        viewModelScope.launch {
            if (settingsRepository == null) {
                settingsRepository = SettingsRepository(application)
            }
            settingsRepository.enabledRoles.collect { enabledRoles ->
                _uiState.update { it.copy(enabledRoles = enabledRoles) }
            }
        }
    }

    private fun loadPrompts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            promptsRepository.getPrompts()
                .onSuccess { prompts ->
                    _uiState.update { it.copy(prompts = prompts, isLoading = false, error = null) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
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
