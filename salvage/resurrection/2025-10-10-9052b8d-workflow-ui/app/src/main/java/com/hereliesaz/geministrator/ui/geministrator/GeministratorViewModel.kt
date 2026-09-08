package com.hereliesaz.geministrator.ui.geministrator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hereliesaz.geministrator.data.SettingsRepository
import com.hereliesaz.geministrator.data.model.geministrator.DelegatedTask
import com.hereliesaz.geministrator.data.model.geministrator.Plan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class GeministratorViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    private val _uiState = MutableStateFlow<GeministratorUiState>(GeministratorUiState.Success())
    val uiState: StateFlow<GeministratorUiState> = _uiState

    init {
        loadEnabledAgents()
    }

    private fun loadEnabledAgents() {
        settingsRepository.enabledAiAgentRoles
            .onEach { enabledRoles ->
                _uiState.update { currentState ->
                    // Since the state is always Success, we can safely cast or check
                    if (currentState is GeministratorUiState.Success) {
                        currentState.copy(availableAgents = enabledRoles?.toList() ?: emptyList())
                    } else {
                        currentState
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun createPlan(userInput: String) {
        // TODO: This is where the logic to call the orchestrator agent would go.
        // For now, I will create a dummy plan to demonstrate the UI.
        val tasks = listOf(
            DelegatedTask("1", "Analyze user request: '$userInput'", "Completed"),
            DelegatedTask("2", "Generate sub-tasks", "In Progress"),
            DelegatedTask("3", "Write code for sub-task 1", "Pending"),
            DelegatedTask("4", "Write tests for sub-task 1", "Pending")
        )
        val plan = Plan("plan1", tasks)
        _uiState.update {
            if (it is GeministratorUiState.Success) {
                it.copy(plan = plan)
            } else {
                it
            }
        }
    }
}

sealed class GeministratorUiState {
    data class Success(
        val plan: Plan? = null,
        val availableAgents: List<String> = emptyList()
    ) : GeministratorUiState()

    data class Error(val message: String) : GeministratorUiState()
}
